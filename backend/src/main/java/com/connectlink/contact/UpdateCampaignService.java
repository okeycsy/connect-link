package com.connectlink.contact;

import com.connectlink.account.AccountService;
import com.connectlink.account.BusinessMember;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.contact.dto.CurrentUpdateCampaignResponse;
import com.connectlink.contact.dto.UpdateCampaignOrganizationResponse;
import com.connectlink.contact.dto.UpdateCampaignProgressResponse;
import com.connectlink.contact.dto.UpdateCampaignReminderRequest;
import com.connectlink.contact.dto.UpdateCampaignReminderResultResponse;
import com.connectlink.contact.dto.UpdateCampaignResponse;
import com.connectlink.contact.dto.UpdateCampaignRunResponse;
import com.connectlink.contact.dto.UpdateCampaignSaveRequest;
import com.connectlink.contact.dto.UpdateCampaignScheduleResponse;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationProperties;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.notification.dto.MailPreviewResponse;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
 *
 * <p>주기({@link UpdateCampaignProperties}, 기본 6개월)가 돌아오면 예약 작업이 주간을 스스로 등록해 변경 요청 메일을 보내고, 진행 중 주간에
 * "현행화 완료" 를 누르지 않은 기관에는 간격마다 리마인드 메일을 다시 보낸다({@link #runSchedule}). 결제원 담당자는 화면에서 바로 리마인드를 보낼 수도
 * 있다.
 */
@Service
@Transactional
public class UpdateCampaignService {

  private final UpdateCampaignRepository campaignRepository;
  private final UpdateCampaignOrganizationRepository organizationRepository;
  private final UpdateCampaignReminderRepository reminderRepository;
  private final UpdateCampaignProperties properties;
  private final ContactRepository contactRepository;
  private final ContactAccessPolicy accessPolicy;
  private final OrganizationService organizationService;
  private final AccountService accountService;
  private final NotificationService notificationService;
  private final NotificationProperties notificationProperties;
  private final Clock clock;

  public UpdateCampaignService(
      UpdateCampaignRepository campaignRepository,
      UpdateCampaignOrganizationRepository organizationRepository,
      UpdateCampaignReminderRepository reminderRepository,
      UpdateCampaignProperties properties,
      ContactRepository contactRepository,
      ContactAccessPolicy accessPolicy,
      OrganizationService organizationService,
      AccountService accountService,
      NotificationService notificationService,
      NotificationProperties notificationProperties,
      Clock clock) {
    this.campaignRepository = campaignRepository;
    this.organizationRepository = organizationRepository;
    this.reminderRepository = reminderRepository;
    this.properties = properties;
    this.contactRepository = contactRepository;
    this.accessPolicy = accessPolicy;
    this.organizationService = organizationService;
    this.accountService = accountService;
    this.notificationService = notificationService;
    this.notificationProperties = notificationProperties;
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
              contactRepository.countByBusinessAndOrganizationId(business, organization.getId()),
              reminderStatsOf(campaign).get(organization.getId()));
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
    notifyOrganizations(campaign, user.name());
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
    reminderRepository.deleteByCampaignId(campaign.getId());
    organizationRepository.deleteByCampaignId(campaign.getId());
    campaignRepository.delete(campaign);
  }

  /** 이 업무의 정기 변경 주기와 다음 요청일. 결제원 담당자만 본다. */
  @Transactional(readOnly = true)
  public UpdateCampaignScheduleResponse schedule(CurrentUser user, Business business) {
    accessPolicy.ensureCanManage(user, business);
    return new UpdateCampaignScheduleResponse(
        properties.isAutoCreate(),
        properties.getCycleMonths(),
        properties.getDurationDays(),
        properties.getReminderIntervalDays(),
        latest(business)
            .map(c -> c.getStartDate().plusMonths(properties.getCycleMonths()))
            .orElse(null));
  }

  // ---- 리마인드 ----

  /**
   * 진행 중 주간에서 아직 "현행화 완료" 를 누르지 않은 기관에 리마인드 메일을 지금 보낸다. 기관을 고르지 않으면 미완료 기관 전부다. 진행 중이 아닌 주간이면 409
   * 다.
   */
  public UpdateCampaignReminderResultResponse sendReminders(
      CurrentUser user, Long id, UpdateCampaignReminderRequest body) {
    UpdateCampaign campaign = loadOpenForReminder(user, id);
    ReminderMessage message = reminderMessage(campaign);
    if (body != null) {
      message =
          new ReminderMessage(
              body.subject() == null ? message.title() : body.subject().strip(),
              body.body() == null ? message.text() : body.body().strip());
    }
    return remind(
        campaign,
        chosenPending(campaign, body == null ? null : body.organizationIds()),
        user.name(),
        message);
  }

  /** 리마인드 메일 미리보기. 지금 보내면 나갈 기본 제목·본문과 받을 기관. 보내기와 같은 규칙으로 기관을 고른다. */
  public MailPreviewResponse reminderPreview(
      CurrentUser user, Long id, List<Long> organizationIds) {
    UpdateCampaign campaign = loadOpenForReminder(user, id);
    ReminderMessage message = reminderMessage(campaign);
    List<BusinessMember> members = accountService.listActiveMembers(campaign.getBusiness());
    List<MailPreviewResponse.Organization> organizations = new ArrayList<>();
    List<String> skipped = new ArrayList<>();
    Set<String> emails = new LinkedHashSet<>();
    for (OrganizationResponse organization : chosenPending(campaign, organizationIds)) {
      Set<String> mine = recipientsOf(campaign.getBusiness(), organization, members);
      if (mine.isEmpty()) {
        skipped.add(organization.name());
        continue;
      }
      emails.addAll(mine);
      organizations.add(
          new MailPreviewResponse.Organization(
              organization.id(), organization.name(), organization.code(), mine.size()));
    }
    return MailPreviewResponse.of(
        message.title(), message.text(), organizations, emails.size(), skipped);
  }

  private UpdateCampaign loadOpenForReminder(CurrentUser user, Long id) {
    UpdateCampaign campaign = load(id);
    accessPolicy.ensureCanManage(user, campaign.getBusiness());
    if (campaign.statusOn(today()) != UpdateCampaignStatus.OPEN) {
      throw BusinessException.conflict(
          "UPDATE_CAMPAIGN_NOT_OPEN", "진행 중인 정기 변경 주간에만 리마인드를 보낼 수 있습니다.");
    }
    return campaign;
  }

  /** 미완료 기관 중 고른 기관. 비우면 미완료 기관 전부다. */
  private List<OrganizationResponse> chosenPending(
      UpdateCampaign campaign, List<Long> organizationIds) {
    Set<Long> chosen =
        organizationIds == null || organizationIds.isEmpty()
            ? null
            : new HashSet<>(organizationIds);
    return pendingOrganizations(campaign).stream()
        .filter(o -> chosen == null || chosen.contains(o.id()))
        .toList();
  }

  /**
   * 예약 작업 한 번. 업무마다 (1) 최근 주간이 끝났고 그 시작일에서 주기가 지났으면 새 주간을 등록해 변경 요청 메일을 보내고, (2) 진행 중 주간의 미완료 기관 중
   * 요청 메일이나 앞 리마인드에서 간격이 지난 기관에 리마인드 메일을 보낸다. 등록한 주간이 하나도 없는 업무는 첫 주간을 결제원 담당자가 등록할 때까지 건너뛴다.
   */
  public UpdateCampaignRunResponse runSchedule() {
    int created = 0;
    int reminded = 0;
    for (Business business : Business.values()) {
      if (createIfDue(business)) {
        created++;
      }
      Optional<UpdateCampaign> open = findOpen(business);
      if (open.isPresent()) {
        reminded += remindDue(open.get());
      }
    }
    return new UpdateCampaignRunResponse(created, reminded);
  }

  private boolean createIfDue(Business business) {
    if (!properties.isAutoCreate()) {
      return false;
    }
    Optional<UpdateCampaign> latest = latest(business);
    LocalDate today = today();
    if (latest.isEmpty()
        || latest.get().statusOn(today) != UpdateCampaignStatus.CLOSED
        || today.isBefore(latest.get().getStartDate().plusMonths(properties.getCycleMonths()))) {
      return false;
    }
    String half = today.getMonthValue() <= 6 ? "상반기" : "하반기";
    UpdateCampaign campaign =
        campaignRepository.saveAndFlush(
            UpdateCampaign.autoCreated(
                business,
                today.getYear() + "년 " + half + " 담당자 정기 변경",
                today,
                today.plusDays(Math.max(1, properties.getDurationDays()) - 1L),
                AUTO_REGISTRANT));
    notifyOrganizations(campaign, AUTO_REGISTRANT);
    return true;
  }

  /** 요청 메일(주간 등록일과 시작일 중 늦은 날)이나 마지막 리마인드에서 간격이 지난 미완료 기관에 보낸다. */
  private int remindDue(UpdateCampaign campaign) {
    LocalDate today = today();
    LocalDate requested = campaign.getStartDate();
    if (campaign.getCreatedAt() != null
        && campaign.getCreatedAt().toLocalDate().isAfter(requested)) {
      requested = campaign.getCreatedAt().toLocalDate();
    }
    Map<Long, ReminderStats> stats = reminderStatsOf(campaign);
    List<OrganizationResponse> due = new ArrayList<>();
    for (OrganizationResponse organization : pendingOrganizations(campaign)) {
      LocalDate last = requested;
      ReminderStats s = stats.get(organization.id());
      if (s != null && s.lastSentAt().toLocalDate().isAfter(last)) {
        last = s.lastSentAt().toLocalDate();
      }
      if (!today.isBefore(last.plusDays(properties.getReminderIntervalDays()))) {
        due.add(organization);
      }
    }
    return remind(campaign, due, null, reminderMessage(campaign)).organizationCount();
  }

  /** 리마인드 메일 제목과 본문 한 벌. */
  private record ReminderMessage(String title, String text) {}

  /** 리마인드 메일의 기본 문구. 예약 작업은 이 문구로 보내고, 화면에서 보낼 때는 미리보기로 보여 준 뒤 고친 문구로 보낼 수 있다. */
  private ReminderMessage reminderMessage(UpdateCampaign campaign) {
    Business business = campaign.getBusiness();
    String title =
        "[금융결제원] ("
            + business.getLabel()
            + ") 정기 변경 리마인드: "
            + campaign.getTitle()
            + " (~"
            + campaign.getEndDate()
            + ")";
    String text =
        campaign.getTitle()
            + " 기간("
            + campaign.getStartDate()
            + " ~ "
            + campaign.getEndDate()
            + ")에 우리 기관의 "
            + business.getLabel()
            + " 담당자 현행화 완료가 아직 확인되지 않았습니다. 담당자 정보를 확인해 바뀐 것은 고치고, 모두 맞으면 연락처 화면에서 \"현행화 완료\" 를 눌러 주세요."
            + " 완료하면 리마인드 메일은 더 가지 않습니다.\n"
            + notificationProperties.linkTo("/" + business.getSlug() + "/contacts");
    return new ReminderMessage(title, text);
  }

  /** 기관마다 받을 사람에게 리마인드 메일을 보내고 기록한다. 받을 사람이 없는 기관은 건너뛴다. */
  private UpdateCampaignReminderResultResponse remind(
      UpdateCampaign campaign,
      List<OrganizationResponse> organizations,
      String senderName,
      ReminderMessage message) {
    Business business = campaign.getBusiness();
    List<BusinessMember> members = accountService.listActiveMembers(business);
    int sent = 0;
    int recipients = 0;
    int skipped = 0;
    LocalDateTime now = now();
    for (OrganizationResponse organization : organizations) {
      Set<String> emails = recipientsOf(business, organization, members);
      if (emails.isEmpty()) {
        skipped++;
        continue;
      }
      for (String email : emails) {
        notificationService.publish(
            new NotificationCommand(
                email,
                NotificationType.CONTACT_CHANGE_REMINDER,
                business,
                message.title(),
                message.text(),
                "/" + business.getSlug() + "/contacts",
                Set.of(NotificationChannel.EMAIL)));
      }
      reminderRepository.save(
          new UpdateCampaignReminder(
              campaign,
              organizationService.getByCode(organization.code()),
              now,
              senderName,
              emails.size()));
      sent++;
      recipients += emails.size();
    }
    return new UpdateCampaignReminderResultResponse(sent, recipients, skipped);
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

  /** 주간 이름 대신 "등록" 에 남기는 예약 작업 이름. */
  static final String AUTO_REGISTRANT = "정기 자동 등록";

  private Optional<UpdateCampaign> latest(Business business) {
    return campaignRepository.findByBusinessOrderByStartDateDescIdDesc(business).stream()
        .findFirst();
  }

  /** 대상 기관 중 아직 "현행화 완료" 를 누르지 않은 기관. */
  private List<OrganizationResponse> pendingOrganizations(UpdateCampaign campaign) {
    Set<Long> completed =
        organizationRepository.findByCampaignId(campaign.getId()).stream()
            .filter(UpdateCampaignOrganization::isCompleted)
            .map(r -> r.getOrganization().getId())
            .collect(Collectors.toSet());
    return targetsOf(campaign.getBusiness()).stream()
        .filter(o -> !completed.contains(o.id()))
        .toList();
  }

  /** 그 업무에 참여하는 사용 중 참가기관. */
  private List<OrganizationResponse> targetsOf(Business business) {
    return organizationService.listActive().stream()
        .filter(o -> o.type() == OrganizationType.FINANCE_ORGANIZATION)
        .filter(o -> o.businesses().contains(business))
        .toList();
  }

  /** 한 기관의 변경 요청·리마인드 메일 받는 사람: 그 업무를 맡은 그 기관의 사용 중 계정과 그 기관·업무 담당자 연락처의 회사 이메일(중복 없이, 소문자). */
  private Set<String> recipientsOf(
      Business business, OrganizationResponse organization, List<BusinessMember> members) {
    Set<String> emails = new LinkedHashSet<>();
    members.stream()
        .filter(m -> organization.code().equals(m.organizationCode()))
        .forEach(m -> emails.add(m.email().toLowerCase(Locale.ROOT)));
    contactRepository
        .findByBusinessAndOrganizationIdOrderByRankAscNameAsc(business, organization.id())
        .forEach(c -> emails.add(c.getEmail().toLowerCase(Locale.ROOT)));
    return emails;
  }

  /** 기관별 리마인드 횟수와 마지막 보낸 때. */
  private Map<Long, ReminderStats> reminderStatsOf(UpdateCampaign campaign) {
    Map<Long, ReminderStats> stats = new HashMap<>();
    for (UpdateCampaignReminder r :
        reminderRepository.findByCampaignIdOrderBySentAtAscIdAsc(campaign.getId())) {
      stats.merge(
          r.getOrganization().getId(),
          new ReminderStats(1, r.getSentAt()),
          (a, b) ->
              new ReminderStats(
                  a.count() + 1,
                  a.lastSentAt().isAfter(b.lastSentAt()) ? a.lastSentAt() : b.lastSentAt()));
    }
    return stats;
  }

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

  /** 대상 기관마다 받을 사람(그 기관의 사용 중 계정과 담당자 연락처 이메일)에게 주간을 알린다(변경 요청 메일). 시작 전에 등록해도 등록할 때 한 번 보낸다. */
  private void notifyOrganizations(UpdateCampaign campaign, String registrant) {
    Business business = campaign.getBusiness();
    String period = campaign.getStartDate() + " ~ " + campaign.getEndDate();
    List<BusinessMember> members = accountService.listActiveMembers(business);
    for (OrganizationResponse organization : targetsOf(business)) {
      for (String email : recipientsOf(business, organization, members)) {
        notificationService.publish(
            new NotificationCommand(
                email,
                NotificationType.CONTACT_CHANGE_REQUESTED,
                business,
                "[" + business.getLabel() + "] 정기 변경 주간 " + period,
                campaign.getTitle()
                    + " 기간("
                    + period
                    + ")에 우리 기관 "
                    + business.getLabel()
                    + " 담당자 정보를 확인해 주세요. 바뀐 것은 고치고, 모두 맞으면 연락처 화면에서 \"현행화 완료\" 를 눌러 주세요. 등록: "
                    + registrant,
                "/" + business.getSlug() + "/contacts",
                Set.of(NotificationChannel.EMAIL)));
      }
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
        target == 0 ? 0 : (int) Math.floor(completed * 100.0 / target),
        campaign.isAutoCreated());
  }

  /** 대상 기관(그 업무에 참여하는 사용 중 참가기관)마다의 현행화. 주간에 기록이 있지만 지금은 대상이 아닌 기관(참여를 그만둔 기관)도 기록대로 넣는다. */
  private Progress progressOf(UpdateCampaign campaign) {
    Business business = campaign.getBusiness();
    Map<Long, UpdateCampaignOrganization> rows =
        organizationRepository.findByCampaignId(campaign.getId()).stream()
            .collect(Collectors.toMap(r -> r.getOrganization().getId(), Function.identity()));
    List<OrganizationResponse> targets = targetsOf(business);
    Map<Long, ReminderStats> reminders = reminderStatsOf(campaign);
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
                            contactRepository.countByBusinessAndOrganizationId(business, o.id()),
                            reminders.get(o.id())))
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
                            business, r.getOrganization().getId()),
                        reminders.get(r.getOrganization().getId()))));
    return new Progress(result);
  }

  private static UpdateCampaignOrganizationResponse toOrganizationResponse(
      Organization organization,
      UpdateCampaignOrganization row,
      long contactCount,
      ReminderStats reminders) {
    return toOrganizationResponse(
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        row,
        contactCount,
        reminders);
  }

  private static UpdateCampaignOrganizationResponse toOrganizationResponse(
      Long id,
      String code,
      String name,
      UpdateCampaignOrganization row,
      long contactCount,
      ReminderStats reminders) {
    return new UpdateCampaignOrganizationResponse(
        id,
        code,
        name,
        row != null && row.isCompleted(),
        row == null ? null : row.getCompletedAt(),
        row == null ? null : row.getCompletedByName(),
        row != null && row.isChanged(),
        row == null ? null : row.getChangedAt(),
        contactCount,
        reminders == null ? 0 : reminders.count(),
        reminders == null ? null : reminders.lastSentAt());
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  private record Progress(List<UpdateCampaignOrganizationResponse> organizations) {}

  private record ReminderStats(int count, LocalDateTime lastSentAt) {}
}
