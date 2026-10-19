package com.connectlink.contact;

import com.connectlink.account.AccountService;
import com.connectlink.account.BusinessMember;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.contact.dto.CurrentUpdateCampaignResponse;
import com.connectlink.contact.dto.UpdateCampaignOrganizationResponse;
import com.connectlink.contact.dto.UpdateCampaignProgressResponse;
import com.connectlink.contact.dto.UpdateCampaignResponse;
import com.connectlink.contact.dto.UpdateCampaignSaveRequest;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import com.connectlink.organization.dto.OrganizationResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정기 변경(현행화) 주간. 결제원 담당자가 업무마다 기간을 등록하고, 기관별 현행화 완료·미완료와 전체 진행률, 미확인 기관과 정보 변경 기관을 본다. 기관의 "현행화 완료"
 * 와 담당자 정보 변경은 {@link ContactService} 가 {@link #recordCompletion}, {@link #recordChange} 로 알려 준다.
 */
@Service
@Transactional
public class UpdateCampaignService {

  private final UpdateCampaignRepository campaignRepository;
  private final UpdateCampaignOrganizationRepository organizationRepository;
  private final ContactRepository contactRepository;
  private final ContactAccessPolicy accessPolicy;
  private final OrganizationService organizationService;
  private final AccountService accountService;
  private final NotificationService notificationService;
  private final Clock clock;

  public UpdateCampaignService(
      UpdateCampaignRepository campaignRepository,
      UpdateCampaignOrganizationRepository organizationRepository,
      ContactRepository contactRepository,
      ContactAccessPolicy accessPolicy,
      OrganizationService organizationService,
      AccountService accountService,
      NotificationService notificationService,
      Clock clock) {
    this.campaignRepository = campaignRepository;
    this.organizationRepository = organizationRepository;
    this.contactRepository = contactRepository;
    this.accessPolicy = accessPolicy;
    this.organizationService = organizationService;
    this.accountService = accountService;
    this.notificationService = notificationService;
    this.clock = clock;
  }

  // ---- 조회 ----

  /** 이 업무의 주간 전부, 최근 시작 순. 결제원 담당자만 본다. */
  @Transactional(readOnly = true)
  public List<UpdateCampaignResponse> list(CurrentUser user, Business business) {
    accessPolicy.ensureCanManage(user, business);
    return campaignRepository.findByBusinessOrderByStartDateDescIdDesc(business).stream()
        .map(this::toResponse)
        .toList();
  }

  /** 지금 진행 중인 주간. 연락처 화면을 볼 수 있는 사람 누구나 보고, 기관 담당자는 자기 기관 현행화도 받는다. */
  @Transactional(readOnly = true)
  public CurrentUpdateCampaignResponse current(CurrentUser user, Business business) {
    accessPolicy.ensureCanList(user, business);
    Optional<UpdateCampaign> open = findOpen(business);
    if (open.isEmpty()) {
      return new CurrentUpdateCampaignResponse(null, null);
    }
    UpdateCampaign campaign = open.get();
    UpdateCampaignOrganizationResponse own = null;
    if (accessPolicy.isOrganizationMember(user, business)) {
      Organization organization = organizationService.getByCode(user.organizationCode());
      own =
          toOrganizationResponse(
              organization,
              organizationRepository
                  .findByCampaignIdAndOrganizationId(campaign.getId(), organization.getId())
                  .orElse(null),
              contactRepository.countByBusinessAndOrganizationId(business, organization.getId()));
    }
    return new CurrentUpdateCampaignResponse(toResponse(campaign), own);
  }

  /** 주간 하나의 진행률과 기관별 현행화. 결제원 담당자만 본다. */
  @Transactional(readOnly = true)
  public UpdateCampaignProgressResponse progress(
      CurrentUser user, Long id, UpdateCampaignProgressFilter filter) {
    UpdateCampaign campaign = load(id);
    accessPolicy.ensureCanManage(user, campaign.getBusiness());
    Progress progress = progressOf(campaign);
    UpdateCampaignProgressFilter f = filter == null ? UpdateCampaignProgressFilter.ALL : filter;
    List<UpdateCampaignOrganizationResponse> organizations =
        progress.organizations().stream()
            .filter(
                o ->
                    switch (f) {
                      case ALL -> true;
                      case COMPLETED -> o.completed();
                      case INCOMPLETE -> !o.completed();
                      case CHANGED -> o.changed();
                    })
            .sorted(
                Comparator.comparing(UpdateCampaignOrganizationResponse::completed)
                    .thenComparing(UpdateCampaignOrganizationResponse::organizationName))
            .toList();
    return new UpdateCampaignProgressResponse(toResponse(campaign, progress), organizations);
  }

  // ---- 등록·수정·삭제 ----

  /** 주간을 등록하고, 그 업무를 맡은 참가기관 담당자에게 알림 센터와 메일로 알린다. 같은 업무의 다른 주간과 기간이 겹치면 409 다. */
  public UpdateCampaignResponse create(CurrentUser user, UpdateCampaignSaveRequest body) {
    accessPolicy.ensureCanManage(user, body.business());
    validatePeriod(body.startDate(), body.endDate());
    ensureNoOverlap(body.business(), body.startDate(), body.endDate(), null);
    UpdateCampaign campaign =
        campaignRepository.saveAndFlush(
            new UpdateCampaign(
                body.business(),
                body.title().trim(),
                body.startDate(),
                body.endDate(),
                user.name()));
    notifyOrganizations(campaign, user);
    return toResponse(campaign);
  }

  /** 이름과 기간을 고친다. 업무는 바뀌지 않는다. 끝난 주간은 고치지 않는다. */
  public UpdateCampaignResponse update(CurrentUser user, Long id, UpdateCampaignSaveRequest body) {
    UpdateCampaign campaign = load(id);
    accessPolicy.ensureCanManage(user, campaign.getBusiness());
    if (campaign.statusOn(today()) == UpdateCampaignStatus.CLOSED) {
      throw BusinessException.conflict("UPDATE_CAMPAIGN_CLOSED", "끝난 정기 변경 주간은 고칠 수 없습니다.");
    }
    validatePeriod(body.startDate(), body.endDate());
    ensureNoOverlap(campaign.getBusiness(), body.startDate(), body.endDate(), campaign.getId());
    campaign.change(body.title().trim(), body.startDate(), body.endDate());
    campaignRepository.flush();
    return toResponse(campaign);
  }

  /** 아직 시작하지 않은 주간만 지운다. 진행 중이거나 끝난 주간은 기관 기록이 있으므로 남긴다. */
  public void delete(CurrentUser user, Long id) {
    UpdateCampaign campaign = load(id);
    accessPolicy.ensureCanManage(user, campaign.getBusiness());
    if (campaign.statusOn(today()) != UpdateCampaignStatus.UPCOMING) {
      throw BusinessException.conflict(
          "UPDATE_CAMPAIGN_STARTED", "시작한 정기 변경 주간은 지울 수 없습니다. 기간을 고쳐 일찍 끝내 주세요.");
    }
    organizationRepository.deleteByCampaignId(campaign.getId());
    campaignRepository.delete(campaign);
  }

  // ---- 연락처에서 알려 주는 일 ----

  /** 진행 중인 주간이 있으면 이 기관을 "현행화 완료" 로 남긴다. 주간 이름을 돌려주고, 없으면 비어 있다. */
  Optional<String> recordCompletion(Business business, Organization organization, String byName) {
    if (organization.getType() != OrganizationType.FINANCE_ORGANIZATION) {
      return Optional.empty();
    }
    return findOpen(business)
        .map(
            campaign -> {
              rowOf(campaign, organization).complete(byName, now());
              return campaign.getTitle();
            });
  }

  /** 진행 중인 주간이 있으면 이 기관을 "정보 변경" 으로 남긴다. */
  void recordChange(Business business, Organization organization) {
    if (organization.getType() != OrganizationType.FINANCE_ORGANIZATION) {
      return;
    }
    findOpen(business).ifPresent(campaign -> rowOf(campaign, organization).markChanged(now()));
  }

  /** 진행 중인 주간이 있으면 그 진행 요약. 홈 카드가 쓴다. */
  @Transactional(readOnly = true)
  Optional<UpdateCampaignProgressResponse> openProgress(Business business) {
    return findOpen(business)
        .map(
            campaign -> {
              Progress progress = progressOf(campaign);
              return new UpdateCampaignProgressResponse(
                  toResponse(campaign, progress), progress.organizations());
            });
  }

  // ---- 공통 ----

  Optional<UpdateCampaign> findOpen(Business business) {
    LocalDate today = today();
    return campaignRepository
        .findFirstByBusinessAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            business, today, today);
  }

  private UpdateCampaignOrganization rowOf(UpdateCampaign campaign, Organization organization) {
    return organizationRepository
        .findByCampaignIdAndOrganizationId(campaign.getId(), organization.getId())
        .orElseGet(
            () ->
                organizationRepository.save(
                    new UpdateCampaignOrganization(campaign, organization)));
  }

  private UpdateCampaign load(Long id) {
    return campaignRepository
        .findById(id)
        .orElseThrow(
            () -> BusinessException.notFound("UPDATE_CAMPAIGN_NOT_FOUND", "정기 변경 주간을 찾을 수 없습니다."));
  }

  private static void validatePeriod(LocalDate start, LocalDate end) {
    if (end.isBefore(start)) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "UPDATE_CAMPAIGN_PERIOD_INVALID",
          "endDate",
          "종료일은 시작일과 같거나 뒤여야 합니다.");
    }
  }

  private void ensureNoOverlap(Business business, LocalDate start, LocalDate end, Long selfId) {
    boolean overlaps =
        campaignRepository
            .findByBusinessAndStartDateLessThanEqualAndEndDateGreaterThanEqual(business, end, start)
            .stream()
            .anyMatch(c -> !c.getId().equals(selfId));
    if (overlaps) {
      throw BusinessException.onField(
          HttpStatus.CONFLICT,
          "UPDATE_CAMPAIGN_OVERLAP",
          "startDate",
          business.getLabel() + " 의 다른 정기 변경 주간과 기간이 겹칩니다.");
    }
  }

  /** 그 업무를 맡은 사용 중 참가기관 담당자에게 주간을 알린다. 시작 전에 등록해도 등록할 때 한 번 보낸다. */
  private void notifyOrganizations(UpdateCampaign campaign, CurrentUser actor) {
    Set<String> financeCodes =
        organizationService.listActive().stream()
            .filter(o -> o.type() == OrganizationType.FINANCE_ORGANIZATION)
            .map(OrganizationResponse::code)
            .collect(Collectors.toSet());
    Business business = campaign.getBusiness();
    String period = campaign.getStartDate() + " ~ " + campaign.getEndDate();
    for (BusinessMember member : accountService.listActiveMembers(business)) {
      if (!financeCodes.contains(member.organizationCode())) {
        continue;
      }
      notificationService.publish(
          new NotificationCommand(
              member.email(),
              NotificationType.CONTACT_CHANGE_REQUESTED,
              business,
              "[" + business.getLabel() + "] 정기 변경 주간 " + period,
              campaign.getTitle()
                  + " 기간("
                  + period
                  + ")에 우리 기관 "
                  + business.getLabel()
                  + " 담당자 정보를 확인해 주세요. 바뀐 것은 고치고, 모두 맞으면 연락처 화면에서 \"현행화 완료\" 를 눌러 주세요. 등록: "
                  + actor.name(),
              "/" + business.getSlug() + "/contacts",
              Set.of(NotificationChannel.EMAIL)));
    }
  }

  private UpdateCampaignResponse toResponse(UpdateCampaign campaign) {
    return toResponse(campaign, progressOf(campaign));
  }

  private UpdateCampaignResponse toResponse(UpdateCampaign campaign, Progress progress) {
    UpdateCampaignStatus status = campaign.statusOn(today());
    int target = progress.organizations().size();
    int completed =
        (int)
            progress.organizations().stream()
                .filter(UpdateCampaignOrganizationResponse::completed)
                .count();
    int changed =
        (int)
            progress.organizations().stream()
                .filter(UpdateCampaignOrganizationResponse::changed)
                .count();
    return new UpdateCampaignResponse(
        campaign.getId(),
        campaign.getBusiness(),
        campaign.getBusiness().getLabel(),
        campaign.getTitle(),
        campaign.getStartDate(),
        campaign.getEndDate(),
        status,
        status.getLabel(),
        campaign.getRegistrantName(),
        campaign.getCreatedAt(),
        target,
        completed,
        changed,
        target == 0 ? 0 : (int) Math.floor(completed * 100.0 / target));
  }

  /** 대상 기관(그 업무에 참여하는 사용 중 참가기관)마다의 현행화. 주간에 기록이 있지만 지금은 대상이 아닌 기관(참여를 그만둔 기관)도 기록대로 넣는다. */
  private Progress progressOf(UpdateCampaign campaign) {
    Business business = campaign.getBusiness();
    Map<Long, UpdateCampaignOrganization> rows =
        organizationRepository.findByCampaignId(campaign.getId()).stream()
            .collect(Collectors.toMap(r -> r.getOrganization().getId(), Function.identity()));
    List<OrganizationResponse> targets =
        organizationService.listActive().stream()
            .filter(o -> o.type() == OrganizationType.FINANCE_ORGANIZATION)
            .filter(o -> o.businesses().contains(business))
            .toList();
    Set<Long> targetIds =
        targets.stream().map(OrganizationResponse::id).collect(Collectors.toSet());
    List<UpdateCampaignOrganizationResponse> result =
        new ArrayList<>(
            targets.stream()
                .map(
                    o ->
                        toOrganizationResponse(
                            o.id(),
                            o.code(),
                            o.name(),
                            rows.get(o.id()),
                            contactRepository.countByBusinessAndOrganizationId(business, o.id())))
                .toList());
    rows.values().stream()
        .filter(r -> !targetIds.contains(r.getOrganization().getId()))
        .forEach(
            r ->
                result.add(
                    toOrganizationResponse(
                        r.getOrganization(),
                        r,
                        contactRepository.countByBusinessAndOrganizationId(
                            business, r.getOrganization().getId()))));
    return new Progress(result);
  }

  private static UpdateCampaignOrganizationResponse toOrganizationResponse(
      Organization organization, UpdateCampaignOrganization row, long contactCount) {
    return toOrganizationResponse(
        organization.getId(), organization.getCode(), organization.getName(), row, contactCount);
  }

  private static UpdateCampaignOrganizationResponse toOrganizationResponse(
      Long id, String code, String name, UpdateCampaignOrganization row, long contactCount) {
    return new UpdateCampaignOrganizationResponse(
        id,
        code,
        name,
        row != null && row.isCompleted(),
        row == null ? null : row.getCompletedAt(),
        row == null ? null : row.getCompletedByName(),
        row != null && row.isChanged(),
        row == null ? null : row.getChangedAt(),
        contactCount);
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  private record Progress(List<UpdateCampaignOrganizationResponse> organizations) {}
}
