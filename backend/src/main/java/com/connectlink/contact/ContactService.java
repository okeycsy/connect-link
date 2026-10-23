package com.connectlink.contact;

import com.connectlink.account.AccountApprovedEvent;
import com.connectlink.account.AccountService;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.NetworkZoneResolver;
import com.connectlink.common.PageResponse;
import com.connectlink.contact.dto.ContactCountsResponse;
import com.connectlink.contact.dto.ContactOrganizationConfirmRequest;
import com.connectlink.contact.dto.ContactOrganizationConfirmResponse;
import com.connectlink.contact.dto.ContactOrganizationResponse;
import com.connectlink.contact.dto.ContactPermissions;
import com.connectlink.contact.dto.ContactResponse;
import com.connectlink.contact.dto.ContactSaveRequest;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.history.dto.FieldChange;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당자 연락처. 목록·상세 조회, 등록·수정·삭제, "변동 없음" 확인(최근 확인일 갱신), 기관 단위 현행화 완료, 6개월 미확인 담당자와 알림을 맡는다. 누가 무엇을
 * 하는지는 {@link ContactAccessPolicy}, 정기 변경 주간은 {@link UpdateCampaignService} 가 맡는다.
 *
 * <p>내부망 전용(명세): 결제원 담당자의 등록·수정·삭제·확인, 검색·필터, 엑셀 다운로드. 기관 담당자의 수정·확인은 인터넷망에서도 된다.
 */
@Service
@Transactional
public class ContactService {

  /** 최근 확인일에서 이만큼 지나면 확인 알림을 띄운다(명세 "6개월"). */
  static final Period RECONFIRM_AFTER = Period.ofMonths(6);

  /** 엑셀 한 파일에 넣는 담당자 수 상한. 한 업무 담당자가 이보다 많을 일은 드물다. */
  private static final int EXCEL_LIMIT = 5000;

  private final ContactRepository repository;
  private final ContactAccessPolicy accessPolicy;
  private final OrganizationService organizationService;
  private final AccountService accountService;
  private final ChangeHistoryService changeHistoryService;
  private final NotificationService notificationService;
  private final UpdateCampaignService campaignService;
  private final NetworkZoneResolver networkZoneResolver;
  private final Clock clock;

  public ContactService(
      ContactRepository repository,
      ContactAccessPolicy accessPolicy,
      OrganizationService organizationService,
      AccountService accountService,
      ChangeHistoryService changeHistoryService,
      NotificationService notificationService,
      UpdateCampaignService campaignService,
      NetworkZoneResolver networkZoneResolver,
      Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.organizationService = organizationService;
    this.accountService = accountService;
    this.changeHistoryService = changeHistoryService;
    this.notificationService = notificationService;
    this.campaignService = campaignService;
    this.networkZoneResolver = networkZoneResolver;
    this.clock = clock;
  }

  /** 목록의 검색·필터 조건. 하나라도 있으면 내부망 전용이다. */
  public record Filter(String keyword, Long organizationId, JobRole jobRole, boolean needsConfirm) {

    public static final Filter NONE = new Filter(null, null, null, false);

    boolean isEmpty() {
      return (keyword == null || keyword.isBlank())
          && organizationId == null
          && jobRole == null
          && !needsConfirm;
    }
  }

  // ---- 조회 ----

  /**
   * 한 업무의 담당자. 결제원 담당자(와 운영·상황실·대외계)는 전 기관을, 기관 담당자는 결제원 담당자와 자기 기관만 본다. 결제원이 맨 위이고 기관 이름순, 기관 안에서는
   * 주담당 → 부담당, 이름순이다. 검색·필터는 내부망에서만 쓴다.
   */
  @Transactional(readOnly = true)
  public PageResponse<ContactResponse> list(
      CurrentUser user, Business business, Filter filter, Pageable pageable) {
    Specification<Contact> spec = scope(user, business).and(filterOf(filter));
    Pageable sorted =
        PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(), ContactSpecifications.DEFAULT_SORT);
    LocalDateTime now = now();
    boolean internal = networkZoneResolver.isInternal();
    return PageResponse.from(
        repository.findAll(spec, sorted), c -> toResponse(user, c, now, internal));
  }

  /** 목록 머리의 건수. 볼 수 있는 범위 전체이며 검색·필터와 무관하다. */
  @Transactional(readOnly = true)
  public ContactCountsResponse counts(CurrentUser user, Business business) {
    Specification<Contact> scope = scope(user, business);
    return new ContactCountsResponse(
        repository.count(scope),
        repository.count(
            scope.and(ContactSpecifications.filter(null, null, null, reconfirmThreshold()))));
  }

  @Transactional(readOnly = true)
  public ContactResponse get(CurrentUser user, Long id) {
    Contact contact = load(id);
    accessPolicy.ensureCanView(user, contact);
    return toResponse(user, contact);
  }

  /**
   * 이 업무에서 담당자를 등록하거나 목록을 거를 기관. 전 기관을 보는 사람에게는 결제원과 그 업무에 참여하는 사용 중 참가기관 전부, 기관 담당자에게는 결제원과 자기
   * 기관이다. 결제원이 먼저, 그다음 이름순이다.
   */
  @Transactional(readOnly = true)
  public List<ContactOrganizationResponse> organizations(CurrentUser user, Business business) {
    accessPolicy.ensureCanList(user, business);
    boolean all = accessPolicy.canViewAll(user, business);
    return organizationService.listActive().stream()
        .filter(
            o ->
                o.type() == OrganizationType.KFTC
                    || (o.type() == OrganizationType.FINANCE_ORGANIZATION
                        && o.businesses().contains(business)
                        && (all || o.code().equals(user.organizationCode()))))
        .map(o -> new ContactOrganizationResponse(o.id(), o.code(), o.name(), o.type()))
        .toList();
  }

  /** 로그인한 사람 본인의 담당자 행(맡은 업무마다 하나). 내 정보 화면이 쓴다. */
  @Transactional(readOnly = true)
  public List<ContactResponse> mine(CurrentUser user) {
    LocalDateTime now = now();
    boolean internal = networkZoneResolver.isInternal();
    return repository.findByEmailAndOrganizationCode(user.email(), user.organizationCode()).stream()
        .sorted(Comparator.comparing(Contact::getBusiness))
        .map(c -> toResponse(user, c, now, internal))
        .toList();
  }

  /**
   * 로그인할 때 확인 알림을 띄울 담당자: 최근 확인일에서 6개월이 지난 본인 행과, 기관 담당자면 자기 기관의 맡은 업무 담당자(계정이 없는 후임자 등). 결제원
   * 담당자에게는 본인 행만이다(전 기관을 다 띄우지 않는다). 업무 순, 이름순이다.
   */
  @Transactional(readOnly = true)
  public List<ContactResponse> reconfirmDue(CurrentUser user) {
    LocalDateTime threshold = reconfirmThreshold();
    Map<Long, Contact> due = new LinkedHashMap<>();
    repository.findByEmailAndOrganizationCode(user.email(), user.organizationCode()).stream()
        .filter(c -> c.getLastConfirmedAt().isBefore(threshold))
        .forEach(c -> due.put(c.getId(), c));
    if (user.organizationType() == OrganizationType.FINANCE_ORGANIZATION && !user.admin()) {
      Organization own = organizationService.getByCode(user.organizationCode());
      for (Business business : user.businesses()) {
        repository
            .findByBusinessAndOrganizationIdOrderByRankAscNameAsc(business, own.getId())
            .stream()
            .filter(c -> c.getLastConfirmedAt().isBefore(threshold))
            .forEach(c -> due.putIfAbsent(c.getId(), c));
      }
    }
    LocalDateTime now = now();
    boolean internal = networkZoneResolver.isInternal();
    return due.values().stream()
        .filter(c -> accessPolicy.canEdit(user, c))
        .sorted(Comparator.comparing(Contact::getBusiness).thenComparing(Contact::getName))
        .map(c -> toResponse(user, c, now, internal))
        .toList();
  }

  /** 담당자 목록 엑셀. 내부망 전용이고 화면 목록과 같은 범위·조건이다. */
  @Transactional(readOnly = true)
  public ContactExcel excel(CurrentUser user, Business business, Filter filter) {
    networkZoneResolver.ensureInternal("담당자 엑셀 다운로드");
    Specification<Contact> spec = scope(user, business).and(filterOf(filter));
    List<Contact> rows =
        repository
            .findAll(spec, PageRequest.of(0, EXCEL_LIMIT, ContactSpecifications.DEFAULT_SORT))
            .getContent();
    return ContactExcel.of(business, rows, now(), reconfirmThreshold());
  }

  // ---- 등록·수정·삭제·확인 ----

  /**
   * 담당자를 등록한다. 결제원 담당자는 결제원이나 그 업무 참가기관을 고르고(내부망), 기관 담당자는 자기 기관에 후임자 등을 등록한다. 같은 기관·업무에 같은 회사 이메일이
   * 있으면 409 다. 최근 확인일은 지금이다.
   */
  public ContactResponse create(CurrentUser user, ContactSaveRequest body) {
    Business business = body.business();
    Organization organization = organizationFor(user, business, body.organizationId());
    accessPolicy.ensureCanCreate(user, business, organization);
    ensureInternalIfKftc(user, "담당자 등록");
    Contact.Content content = contentOf(body);
    ensureNoDuplicate(organization, business, content.email(), null);
    Contact contact =
        repository.saveAndFlush(
            Contact.register(organization, business, content, user.name(), now()));
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.created(
            HistoryTargetType.CONTACT,
            contact.getId(),
            business,
            summaryOf(contact, "등록"),
            Snapshot.of(contact).diff(Snapshot.EMPTY, true)));
    campaignService.recordChange(business, organization);
    return toResponse(user, contact);
  }

  /**
   * 담당자 정보를 고친다. 저장하면 바뀐 것이 없어도 최근 확인일이 갱신된다(확인한 것으로 본다). 바뀐 항목은 변경 전·후 이력으로 남고, 정기 변경 주간이면 그 기관을
   * "정보 변경" 으로 남긴다. 기관과 업무는 바뀌지 않는다.
   */
  public ContactResponse update(CurrentUser user, Long id, ContactSaveRequest body) {
    Contact contact = load(id);
    accessPolicy.ensureCanEdit(user, contact);
    ensureInternalIfKftc(user, "담당자 수정");
    Contact.Content content = contentOf(body);
    ensureNoDuplicate(
        contact.getOrganization(), contact.getBusiness(), content.email(), contact.getId());
    Snapshot before = Snapshot.of(contact);
    contact.change(content);
    contact.confirm(user.name(), now());
    List<FieldChange> changes = Snapshot.of(contact).diff(before, false);
    repository.flush();
    if (!changes.isEmpty()) {
      changeHistoryService.record(
          user,
          ChangeHistoryCommand.updated(
              HistoryTargetType.CONTACT,
              contact.getId(),
              contact.getBusiness(),
              summaryOf(contact, "수정"),
              changes));
      campaignService.recordChange(contact.getBusiness(), contact.getOrganization());
    }
    return toResponse(user, contact);
  }

  /** "변동 없음" 확인. 내용은 그대로 두고 최근 확인일만 지금으로 한다. 이력은 남기지 않는다(바뀐 것이 없다). */
  public ContactResponse confirm(CurrentUser user, Long id) {
    Contact contact = load(id);
    accessPolicy.ensureCanEdit(user, contact);
    ensureInternalIfKftc(user, "최근 확인");
    contact.confirm(user.name(), now());
    repository.flush();
    return toResponse(user, contact);
  }

  /**
   * 한 업무의 전 기관 담당자 연락처(주담당 먼저, 이름순). 다른 모듈이 담당자를 고를 때 쓰는 읽기 전용 목록이라 권한은 부르는 쪽이 본다. 계정이 없는 담당자도 들어
   * 있다.
   */
  @Transactional(readOnly = true)
  public List<BusinessContact> listByBusiness(Business business) {
    return repository.findByBusinessOrderByRankAscNameAsc(business).stream()
        .map(
            c ->
                new BusinessContact(
                    c.getId(),
                    c.getOrganization().getId(),
                    c.getEmail(),
                    c.getName(),
                    c.getDepartment(),
                    c.getTeam(),
                    c.getJobTitle(),
                    c.getJobRole(),
                    c.jobRoleLabel(),
                    c.getRank(),
                    c.getSubBusiness()))
        .toList();
  }

  /** 그 업무 결제원 담당자가 내부망에서 지운다. 삭제 이력을 남기고, 정기 변경 주간이면 그 기관을 "정보 변경" 으로 남긴다. */
  public void delete(CurrentUser user, Long id) {
    Contact contact = load(id);
    accessPolicy.ensureCanDelete(user, contact);
    networkZoneResolver.ensureInternal("담당자 삭제");
    recordDeleted(user, contact);
    campaignService.recordChange(contact.getBusiness(), contact.getOrganization());
    repository.delete(contact);
  }

  /**
   * 기관 하나의 한 업무 담당자 전부를 "변동 없음" 으로 확인한다. 정기 변경 주간이 진행 중이면 그 기관을 "현행화 완료" 로 남긴다. 담당자가 한 명도 없으면 409
   * 다(먼저 등록한다).
   */
  public ContactOrganizationConfirmResponse confirmOrganization(
      CurrentUser user, ContactOrganizationConfirmRequest body) {
    Business business = body.business();
    Organization organization = organizationFor(user, business, body.organizationId());
    if (!accessPolicy.canCreate(user, business, organization)) {
      throw new AccessDeniedException(ContactAccessPolicy.EDIT_DENIED);
    }
    ensureInternalIfKftc(user, "현행화 완료");
    List<Contact> contacts =
        repository.findByBusinessAndOrganizationIdOrderByRankAscNameAsc(
            business, organization.getId());
    if (contacts.isEmpty()) {
      throw BusinessException.conflict(
          "CONTACT_NONE",
          organization.getName() + " 의 " + business.getLabel() + " 담당자가 없습니다. 먼저 등록해 주세요.");
    }
    LocalDateTime now = now();
    contacts.forEach(c -> c.confirm(user.name(), now));
    String title =
        campaignService.recordCompletion(business, organization, user.name()).orElse(null);
    return new ContactOrganizationConfirmResponse(contacts.size(), title);
  }

  // ---- 계정 연동 ----

  /**
   * 가입이 승인된 계정의 담당 업무마다 담당자 행을 만든다(가입 때 받은 부서·연락처를 넘겨받는다). 같은 기관·업무에 같은 이메일 행이 이미 있으면(후임자로 먼저 등록된
   * 경우) 그대로 둔다. 같은 직무의 주담당이 없으면 주담당, 있으면 부담당이다.
   */
  void createFromApprovedAccount(AccountApprovedEvent event) {
    Organization organization = organizationService.getByCode(event.organizationCode());
    if (organization.getType() == OrganizationType.USE_ORGANIZATION) {
      return;
    }
    LocalDateTime now = now();
    event
        .subBusinesses()
        .forEach(
            (business, subBusiness) -> {
              if (repository.existsByOrganizationIdAndBusinessAndEmail(
                  organization.getId(), business, event.email())) {
                return;
              }
              boolean hasPrimary =
                  repository
                      .findByBusinessAndOrganizationIdOrderByRankAscNameAsc(
                          business, organization.getId())
                      .stream()
                      .anyMatch(
                          c ->
                              c.getRank() == ContactRank.PRIMARY
                                  && c.getJobRole() == event.jobRole());
              Contact contact =
                  repository.saveAndFlush(
                      Contact.register(
                          organization,
                          business,
                          new Contact.Content(
                              blankToNull(subBusiness),
                              event.name(),
                              event.department(),
                              blankToNull(event.team()),
                              blankToNull(event.jobTitle()),
                              event.jobRole(),
                              event.itRoles(),
                              hasPrimary ? ContactRank.SECONDARY : ContactRank.PRIMARY,
                              event.extensionPhone(),
                              blankToNull(event.mobilePhone()),
                              normalizeEmail(event.email())),
                          event.approvedBy().name(),
                          now));
              changeHistoryService.record(
                  event.approvedBy(),
                  ChangeHistoryCommand.created(
                      HistoryTargetType.CONTACT,
                      contact.getId(),
                      business,
                      summaryOf(contact, "등록(가입 승인)"),
                      Snapshot.of(contact).diff(Snapshot.EMPTY, true)));
              campaignService.recordChange(business, organization);
            });
  }

  /** 탈퇴한 계정의 본인 행을 지운다(CLAUDE.md 용어집 "탈퇴"). 삭제 이력을 남긴다. */
  void removeWithdrawn(CurrentUser actor, String email, String organizationCode) {
    for (Contact contact : repository.findByEmailAndOrganizationCode(email, organizationCode)) {
      recordDeleted(actor, contact);
      campaignService.recordChange(contact.getBusiness(), contact.getOrganization());
      repository.delete(contact);
    }
  }

  /** 이 사람 말고는 그 기관의 그 업무 담당자가 없는 업무. 탈퇴 전 후임자 규칙이 쓴다. */
  @Transactional(readOnly = true)
  List<Business> soleContactBusinesses(String email, String organizationCode) {
    return repository.findByEmailAndOrganizationCode(email, organizationCode).stream()
        .filter(c -> c.getOrganization().getType() == OrganizationType.FINANCE_ORGANIZATION)
        .filter(
            c ->
                repository.countByBusinessAndOrganizationId(
                        c.getBusiness(), c.getOrganization().getId())
                    <= 1)
        .map(Contact::getBusiness)
        .sorted()
        .toList();
  }

  // ---- 6개월 미확인 알림 ----

  /**
   * 최근 확인일에서 6개월이 지났고 이번 미확인 기간에 아직 알리지 않은 담당자에게 알림 센터와 메일로 확인을 요청한다. 계정이 있는(사용 중) 담당자에게만 보낸다. 예약
   * 작업이 하루 한 번 부른다.
   *
   * @return 알린 담당자 수
   */
  public int notifyReconfirmDue() {
    LocalDateTime now = now();
    int sent = 0;
    for (Contact contact : repository.findByLastConfirmedAtBefore(reconfirmThreshold())) {
      if (!contact.isReconfirmNotificationPending()
          || !accountService.isActive(contact.getEmail())) {
        continue;
      }
      Business business = contact.getBusiness();
      notificationService.publish(
          new NotificationCommand(
              contact.getEmail(),
              NotificationType.CONTACT_RECONFIRM_DUE,
              business,
              "[" + business.getLabel() + "] 담당자 정보를 확인해 주세요",
              contact.getOrganization().getName()
                  + " "
                  + contact.getName()
                  + " 님의 "
                  + business.getLabel()
                  + " 담당자 정보 최근 확인일("
                  + contact.getLastConfirmedAt().toLocalDate()
                  + ")에서 6개월이 지났습니다. 연결이에서 고치거나 \"변동 없음\" 으로 확인해 주세요.",
              "/" + business.getSlug() + "/contacts?id=" + contact.getId(),
              Set.of(NotificationChannel.EMAIL)));
      contact.markReconfirmNotified(now);
      sent++;
    }
    return sent;
  }

  // ---- 응답 ----

  ContactResponse toResponse(CurrentUser user, Contact contact) {
    return toResponse(user, contact, now(), networkZoneResolver.isInternal());
  }

  private ContactResponse toResponse(
      CurrentUser user, Contact c, LocalDateTime now, boolean internal) {
    boolean kftcSide = accessPolicy.isKftcSide(user);
    Organization organization = c.getOrganization();
    return new ContactResponse(
        c.getId(),
        c.getBusiness(),
        c.getBusiness().getLabel(),
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        organization.getType(),
        c.getSubBusiness(),
        c.getName(),
        c.getDepartment(),
        c.getTeam(),
        c.getJobTitle(),
        c.getJobRole(),
        c.jobRoleLabel(),
        List.copyOf(c.getItRoles()),
        c.getRank(),
        c.getRank().getLabel(),
        c.getExtensionPhone(),
        c.getMobilePhone(),
        c.getEmail(),
        c.getLastConfirmedAt(),
        c.getLastConfirmedByName(),
        c.getLastConfirmedAt().isBefore(now.minus(RECONFIRM_AFTER)),
        c.getEmail().equals(user.email()),
        new ContactPermissions(
            accessPolicy.canEdit(user, c) && (!kftcSide || internal),
            accessPolicy.canDelete(user, c) && internal));
  }

  // ---- 공통 ----

  Contact load(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("CONTACT_NOT_FOUND", "담당자를 찾을 수 없습니다."));
  }

  /** 사용자가 볼 수 있는 범위. */
  private Specification<Contact> scope(CurrentUser user, Business business) {
    if (accessPolicy.canViewAll(user, business)) {
      return ContactSpecifications.scope(business, null);
    }
    if (accessPolicy.isOrganizationMember(user, business)) {
      return ContactSpecifications.scope(business, user.organizationCode());
    }
    throw new AccessDeniedException(ContactAccessPolicy.VIEW_DENIED);
  }

  /** 검색·필터 조건. 조건이 있으면 내부망 전용이다(명세). */
  private Specification<Contact> filterOf(Filter filter) {
    if (filter == null || filter.isEmpty()) {
      return ContactSpecifications.filter(null, null, null, null);
    }
    networkZoneResolver.ensureInternal("담당자 검색·필터");
    return ContactSpecifications.filter(
        filter.keyword(),
        filter.organizationId(),
        filter.jobRole(),
        filter.needsConfirm() ? reconfirmThreshold() : null);
  }

  /** 등록·현행화 완료의 기관. 결제원 담당자는 고른 기관(결제원이거나 그 업무에 참여하는 사용 중 참가기관), 기관 담당자는 자기 기관이다(고른 값을 무시한다). */
  private Organization organizationFor(CurrentUser user, Business business, Long organizationId) {
    if (!accessPolicy.canManage(user, business)) {
      if (!accessPolicy.isOrganizationMember(user, business)) {
        throw new AccessDeniedException(ContactAccessPolicy.CREATE_DENIED);
      }
      return organizationService.getByCode(user.organizationCode());
    }
    if (organizationId == null) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "CONTACT_ORGANIZATION_REQUIRED", "organizationId", "기관을 골라 주세요.");
    }
    Organization organization;
    try {
      organization = organizationService.getActive(organizationId);
    } catch (BusinessException e) {
      throw invalidOrganization(business);
    }
    boolean valid =
        organization.getType() == OrganizationType.KFTC
            || (organization.getType() == OrganizationType.FINANCE_ORGANIZATION
                && organization.participatesIn(business));
    if (!valid) {
      throw invalidOrganization(business);
    }
    return organization;
  }

  private static BusinessException invalidOrganization(Business business) {
    return BusinessException.onField(
        HttpStatus.BAD_REQUEST,
        "CONTACT_ORGANIZATION_INVALID",
        "organizationId",
        "기관은 결제원이거나 " + business.getLabel() + " 에 참여하는 사용 중 참가기관이어야 합니다.");
  }

  private void ensureInternalIfKftc(CurrentUser user, String feature) {
    if (accessPolicy.isKftcSide(user)) {
      networkZoneResolver.ensureInternal(feature);
    }
  }

  private void ensureNoDuplicate(
      Organization organization, Business business, String email, Long selfId) {
    boolean duplicate =
        repository
            .findByBusinessAndOrganizationIdOrderByRankAscNameAsc(business, organization.getId())
            .stream()
            .anyMatch(c -> c.getEmail().equals(email) && !c.getId().equals(selfId));
    if (duplicate) {
      throw BusinessException.onField(
          HttpStatus.CONFLICT,
          "CONTACT_DUPLICATE",
          "email",
          organization.getName() + " 의 " + business.getLabel() + " 담당자에 같은 회사 이메일이 이미 있습니다.");
    }
  }

  private void recordDeleted(CurrentUser actor, Contact contact) {
    changeHistoryService.record(
        actor,
        ChangeHistoryCommand.deleted(
            HistoryTargetType.CONTACT,
            contact.getId(),
            contact.getBusiness(),
            summaryOf(contact, "삭제"),
            FieldChanges.compare()
                .value("name", "이름", contact.getName(), null)
                .value("jobRole", "직무", contact.jobRoleLabel(), null)
                .value("rank", "주/부담당", contact.getRank(), null)
                .toList()));
  }

  /** 양식 값을 저장할 모양으로. 글은 앞뒤 공백을 떼고 빈 글은 null, 이메일은 소문자, 현업이면 전산 세부 직무를 버린다. */
  private static Contact.Content contentOf(ContactSaveRequest body) {
    Set<ItRole> itRoles = EnumSet.noneOf(ItRole.class);
    if (body.jobRole() == JobRole.IT) {
      if (body.itRoles() == null || body.itRoles().isEmpty()) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "CONTACT_IT_ROLE_REQUIRED",
            "itRoles",
            "전산 세부 직무를 하나 이상 골라 주세요.");
      }
      itRoles.addAll(body.itRoles());
    }
    return new Contact.Content(
        blankToNull(body.subBusiness()),
        body.name().trim(),
        body.department().trim(),
        blankToNull(body.team()),
        blankToNull(body.jobTitle()),
        body.jobRole(),
        itRoles,
        body.rank(),
        body.extensionPhone().trim(),
        blankToNull(body.mobilePhone()),
        normalizeEmail(body.email()));
  }

  private static String summaryOf(Contact contact, String what) {
    return contact.getOrganization().getName() + " " + contact.getName() + " 담당자 " + what;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  LocalDateTime reconfirmThreshold() {
    return now().minus(RECONFIRM_AFTER);
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  /** 이력 비교용 값. 화면에 보일 글자다. 휴대폰과 이메일은 조회할 때 {@link ContactHistoryAccess} 가 가린다. */
  private record Snapshot(
      String subBusiness,
      String name,
      String department,
      String team,
      String jobTitle,
      String jobRole,
      String rank,
      String extensionPhone,
      String mobilePhone,
      String email) {

    static final Snapshot EMPTY =
        new Snapshot(null, null, null, null, null, null, null, null, null, null);

    static Snapshot of(Contact c) {
      return new Snapshot(
          c.getSubBusiness(),
          c.getName(),
          c.getDepartment(),
          c.getTeam(),
          c.getJobTitle(),
          c.jobRoleLabel(),
          c.getRank().getLabel(),
          c.getExtensionPhone(),
          c.getMobilePhone(),
          c.getEmail());
    }

    /** {@code before} 에서 이것으로 바뀐 항목. {@code all} 이면 값이 있는 항목을 모두 남긴다(등록 이력). */
    List<FieldChange> diff(Snapshot before, boolean all) {
      List<Object[]> fields = new ArrayList<>();
      fields.add(new Object[] {"name", "이름", before.name, name});
      fields.add(new Object[] {"subBusiness", "세부업무", before.subBusiness, subBusiness});
      fields.add(new Object[] {"department", "부서", before.department, department});
      fields.add(new Object[] {"team", "팀", before.team, team});
      fields.add(new Object[] {"jobTitle", "직급", before.jobTitle, jobTitle});
      fields.add(new Object[] {"jobRole", "직무", before.jobRole, jobRole});
      fields.add(new Object[] {"rank", "주/부담당", before.rank, rank});
      fields.add(new Object[] {"extensionPhone", "내선번호", before.extensionPhone, extensionPhone});
      fields.add(new Object[] {"mobilePhone", "휴대폰번호", before.mobilePhone, mobilePhone});
      fields.add(new Object[] {"email", "회사 이메일", before.email, email});
      FieldChanges changes = FieldChanges.compare();
      for (Object[] f : fields) {
        if (all) {
          if (f[3] != null) {
            changes.value((String) f[0], (String) f[1], null, f[3]);
          }
        } else {
          changes.field((String) f[0], (String) f[1], f[2], f[3]);
        }
      }
      return changes.toList();
    }
  }
}
