package com.connectlink.suspension;

import com.connectlink.account.AccountService;
import com.connectlink.account.BusinessMember;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.attachment.AttachmentService;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import com.connectlink.organization.dto.OrganizationResponse;
import com.connectlink.suspension.dto.SuspensionCountsResponse;
import com.connectlink.suspension.dto.SuspensionDetailResponse;
import com.connectlink.suspension.dto.SuspensionOrganizationResponse;
import com.connectlink.suspension.dto.SuspensionPermissions;
import com.connectlink.suspension.dto.SuspensionRegistrableOrganizationResponse;
import com.connectlink.suspension.dto.SuspensionReviewerResponse;
import com.connectlink.suspension.dto.SuspensionSaveRequest;
import com.connectlink.suspension.dto.SuspensionSummaryResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일시중단. 등록·수정·삭제, 결제원 담당자의 확인 시작과 확정(조치 및 인수인계 사항), 진행 중·예정·지난 목록과 기관별 이력을 맡는다. 누가 무엇을 하는지는 {@link
 * SuspensionAccessPolicy} 가 정한다. 전 영업일 취합 전달은 {@link SuspensionDigestService} 가 한다.
 */
@Service
@Transactional
public class SuspensionService {

  private final SuspensionRepository repository;
  private final SuspensionAccessPolicy accessPolicy;
  private final OrganizationService organizationService;
  private final AccountService accountService;
  private final AttachmentService attachmentService;
  private final ChangeHistoryService changeHistoryService;
  private final SuspensionNotifier notifier;
  private final Clock clock;

  public SuspensionService(
      SuspensionRepository repository,
      SuspensionAccessPolicy accessPolicy,
      OrganizationService organizationService,
      AccountService accountService,
      AttachmentService attachmentService,
      ChangeHistoryService changeHistoryService,
      SuspensionNotifier notifier,
      Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.organizationService = organizationService;
    this.accountService = accountService;
    this.attachmentService = attachmentService;
    this.changeHistoryService = changeHistoryService;
    this.notifier = notifier;
    this.clock = clock;
  }

  // ---- 조회 ----

  /**
   * 한 업무 영역의 일시중단 중 한 시간 구분(없으면 전부). 결제원 담당자는 전 기관을, 기관 담당자는 자기 기관만 본다. {@code organizationId} 는
   * 결제원 담당자의 기관별 이력 보기이고 기관 담당자에게는 쓰지 않는다.
   */
  @Transactional(readOnly = true)
  public PageResponse<SuspensionSummaryResponse> list(
      CurrentUser user,
      Business business,
      SuspensionPeriod period,
      Long organizationId,
      Pageable pageable) {
    LocalDateTime now = now();
    Pageable sorted =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            SuspensionSpecifications.sortOf(period));
    Page<Suspension> page =
        repository.findAll(scope(user, business, organizationId, period, now), sorted);
    return PageResponse.from(page, s -> toSummary(s, now));
  }

  @Transactional(readOnly = true)
  public SuspensionCountsResponse counts(CurrentUser user, Business business, Long organizationId) {
    LocalDateTime now = now();
    Specification<Suspension> notConfirmed =
        (root, query, cb) ->
            root.get("status")
                .in(EnumSet.of(SuspensionStatus.PENDING_REVIEW, SuspensionStatus.IN_REVIEW));
    Specification<Suspension> notEnded =
        (root, query, cb) -> cb.greaterThan(root.get("endAt"), now);
    return new SuspensionCountsResponse(
        repository.count(scope(user, business, organizationId, SuspensionPeriod.CURRENT, now)),
        repository.count(scope(user, business, organizationId, SuspensionPeriod.UPCOMING, now)),
        repository.count(scope(user, business, organizationId, SuspensionPeriod.PAST, now)),
        repository.count(
            scope(user, business, organizationId, null, now).and(notConfirmed).and(notEnded)));
  }

  @Transactional(readOnly = true)
  public SuspensionDetailResponse get(CurrentUser user, Long id) {
    Suspension suspension = load(id);
    accessPolicy.ensureCanView(user, suspension);
    return toDetail(user, suspension);
  }

  /** 이 업무에서 일시중단을 등록하거나 이력을 볼 기관. 결제원 담당자에게는 그 업무에 참여하는 사용 중 참가기관 전부(이름순), 기관 담당자에게는 자기 기관 하나다. */
  @Transactional(readOnly = true)
  public List<SuspensionOrganizationResponse> organizations(CurrentUser user, Business business) {
    if (accessPolicy.canManage(user, business)) {
      return organizationService.listActive().stream()
          .filter(o -> o.type() == OrganizationType.FINANCE_ORGANIZATION)
          .filter(o -> o.businesses().contains(business))
          .map(o -> new SuspensionOrganizationResponse(o.id(), o.code(), o.name()))
          .toList();
    }
    if (accessPolicy.isOrganizationMember(user, business)) {
      Organization own = organizationService.getByCode(user.organizationCode());
      return List.of(new SuspensionOrganizationResponse(own.getId(), own.getCode(), own.getName()));
    }
    throw new AccessDeniedException(SuspensionAccessPolicy.VIEW_DENIED);
  }

  /**
   * 등록 양식에서 고를 수 있는 기관과 각 기관이 참여하는 업무. 등록은 기관 기준이라 업무 영역과 상관없다. 결제원 계정에게는 사용 중 참가기관 전부(이름순), 참가기관
   * 계정에게는 자기 기관 하나다.
   */
  @Transactional(readOnly = true)
  public List<SuspensionRegistrableOrganizationResponse> registrableOrganizations(
      CurrentUser user) {
    accessPolicy.ensureCanRegister(user);
    return organizationService.listActive().stream()
        .filter(o -> o.type() == OrganizationType.FINANCE_ORGANIZATION)
        .filter(o -> accessPolicy.registersAsKftc(user) || o.code().equals(user.organizationCode()))
        .map(
            o ->
                new SuspensionRegistrableOrganizationResponse(
                    o.id(), o.code(), o.name(), o.businesses()))
        .toList();
  }

  // ---- 등록·수정·삭제 ----

  /** 검토 대기로 등록하고 그 업무 결제원 담당자에게 메일·매터모스트로 알린다. 공문은 저장한 뒤 첨부 API 로 올린다. */
  public SuspensionDetailResponse create(CurrentUser user, SuspensionSaveRequest body) {
    accessPolicy.ensureCanRegister(user);
    boolean kftc = accessPolicy.registersAsKftc(user);
    Organization organization =
        kftc
            ? loadOrganization(body.organizationId(), body.business())
            : ownOrganization(user, body.business());
    accessPolicy.ensureCanRegister(user, organization, body.business());
    Suspension.Content content = contentOf(body);
    Suspension suspension =
        repository.saveAndFlush(
            Suspension.register(organization, content, user.email(), user.name(), kftc));
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.created(
            HistoryTargetType.SUSPENSION,
            suspension.getId(),
            suspension.getBusiness(),
            organization.getName() + " 일시중단 등록",
            Snapshot.of(suspension).diff(Snapshot.EMPTY, true)));
    notifier.registered(suspension, kftcRecipients(suspension.getBusiness(), user));
    return toDetail(user, suspension);
  }

  /**
   * 내용을 고친다. 기관 담당자가 확인이 시작된 건을 고치면 결제원이 다시 확인하도록 검토 대기로 돌아간다(결제원 담당자가 고치면 상태는 그대로다). 바뀐 것이 있으면 결제원
   * 담당자에게 매터모스트로, 결제원이 기관의 건을 고쳤으면 등록한 기관 담당자에게 알림 센터로 알린다.
   */
  public SuspensionDetailResponse update(CurrentUser user, Long id, SuspensionSaveRequest body) {
    Suspension suspension = load(id);
    ensureNotCompleted(suspension);
    accessPolicy.ensureCanEdit(user, suspension);
    if (body.business() != suspension.getBusiness()) {
      // 업무를 옮기면 기관이 그 업무에 참여해야 하고 그 업무로도 올릴 수 있어야 한다.
      ensureParticipates(suspension.getOrganization(), body.business());
      accessPolicy.ensureCanRegister(user, suspension.getOrganization(), body.business());
    }
    Snapshot before = Snapshot.of(suspension);
    Business businessBefore = suspension.getBusiness();
    suspension.change(contentOf(body));
    Snapshot after = Snapshot.of(suspension);
    var changes = after.diff(before, false);
    if (changes.isEmpty()) {
      return toDetail(user, suspension);
    }
    // 고칠 수 있는 결제원 계정은 그 업무 담당자이거나 등록한 본인이다. 결제원이 고치면 상태는 그대로다.
    boolean manager = accessPolicy.registersAsKftc(user);
    boolean reopened = false;
    if (!manager && suspension.getStatus() != SuspensionStatus.PENDING_REVIEW) {
      suspension.reopenReview();
      reopened = true;
    }
    repository.flush();
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.SUSPENSION,
            suspension.getId(),
            suspension.getBusiness(),
            suspension.getOrganization().getName() + " 일시중단 수정",
            changes));
    String what = before.changedSummary(after);
    Set<String> recipients = new LinkedHashSet<>(kftcRecipients(businessBefore, user));
    recipients.addAll(kftcRecipients(suspension.getBusiness(), user));
    notifier.changed(suspension, recipients, user.name(), what, reopened);
    if (!suspension.getRegistrantEmail().equals(user.email())) {
      notifier.changedForRegistrant(suspension, user.name(), what);
    }
    return toDetail(user, suspension);
  }

  /** 등록한 본인이 지운다. 공문도 함께 지우고 결제원 담당자에게 매터모스트로 알린다. */
  public void delete(CurrentUser user, Long id) {
    Suspension suspension = load(id);
    ensureNotCompleted(suspension);
    accessPolicy.ensureCanDelete(user, suspension);
    attachmentService.deleteAllOf(AttachmentOwnerType.SUSPENSION, suspension.getId());
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.deleted(
            HistoryTargetType.SUSPENSION,
            suspension.getId(),
            suspension.getBusiness(),
            suspension.getOrganization().getName() + " 일시중단 삭제",
            FieldChanges.compare()
                .value(
                    "period",
                    "중단 일시",
                    SuspensionNotifier.periodText(suspension.getStartAt(), suspension.getEndAt()),
                    null)
                .value("reason", "중단 이유", suspension.getReason(), null)
                .toList()));
    notifier.deleted(suspension, kftcRecipients(suspension.getBusiness(), user), user.name());
    repository.delete(suspension);
  }

  // ---- 검토·확정 ----

  /** 결제원 담당자가 확인을 시작한다. 검토 대기면 검토 중이 된다. 이미 시작했으면 그대로다. */
  public SuspensionDetailResponse startReview(CurrentUser user, Long id) {
    Suspension suspension = load(id);
    ensureNotCompleted(suspension);
    accessPolicy.ensureCanReview(user, suspension);
    SuspensionStatus before = suspension.getStatus();
    if (suspension.startReview(user.email(), user.name(), roleLabelOf(user), now())) {
      repository.flush();
      recordStatus(user, suspension, before, "확인 시작", null);
    }
    return toDetail(user, suspension);
  }

  /** 결제원 담당자가 조치 및 인수인계 사항을 쓰고 확정한다. 확정한 건이면 사항만 고친다. 등록한 사람에게 알린다. */
  public SuspensionDetailResponse confirm(CurrentUser user, Long id, String handover) {
    Suspension suspension = load(id);
    ensureNotCompleted(suspension);
    accessPolicy.ensureCanReview(user, suspension);
    SuspensionStatus before = suspension.getStatus();
    String handoverBefore = suspension.getHandover();
    String text = handover.trim();
    suspension.confirm(text, user.email(), user.name(), roleLabelOf(user), now());
    repository.flush();
    recordStatus(
        user,
        suspension,
        before,
        before == SuspensionStatus.CONFIRMED ? "조치 사항 수정" : "확정",
        Objects.equals(handoverBefore, text) ? null : new String[] {handoverBefore, text});
    if (before != SuspensionStatus.CONFIRMED
        && !suspension.getRegistrantEmail().equals(user.email())) {
      notifier.confirmed(suspension);
    }
    return toDetail(user, suspension);
  }

  /**
   * 종료 시각이 지난 확정 건을 완료로 바꾼다. 예약 작업이 주기적으로 부른다. 확정되지 않은 채 끝난 건은 그대로 두어 목록에서 드러나게 한다.
   *
   * @return 완료로 바꾼 건수
   */
  public int completeEnded() {
    LocalDateTime now = now();
    List<Suspension> ended =
        repository.findByStatusAndEndAtLessThanEqual(SuspensionStatus.CONFIRMED, now);
    ended.forEach(s -> s.complete(now));
    return ended.size();
  }

  // ---- 응답 ----

  static SuspensionSummaryResponse toSummary(Suspension s, LocalDateTime now) {
    SuspensionPeriod period = s.periodAt(now);
    return new SuspensionSummaryResponse(
        s.getId(),
        s.getBusiness(),
        s.getBusiness().getLabel(),
        s.getOrganization().getId(),
        s.getOrganization().getName(),
        s.getStartAt(),
        s.getEndAt(),
        period,
        period.getLabel(),
        s.getStatus(),
        s.getStatus().getLabel(),
        s.getReason(),
        s.getRegistrantName(),
        s.isRegistrantKftc(),
        s.getReviewers().size());
  }

  SuspensionDetailResponse toDetail(CurrentUser user, Suspension s) {
    SuspensionPeriod period = s.periodAt(now());
    boolean review = accessPolicy.canReview(user, s);
    boolean reviewOpen =
        s.getStatus() == SuspensionStatus.PENDING_REVIEW
            || s.getStatus() == SuspensionStatus.IN_REVIEW;
    return new SuspensionDetailResponse(
        s.getId(),
        s.getBusiness(),
        s.getBusiness().getSlug(),
        s.getBusiness().getLabel(),
        s.getOrganization().getId(),
        s.getOrganization().getName(),
        s.getStartAt(),
        s.getEndAt(),
        period,
        period.getLabel(),
        s.getStatus(),
        s.getStatus().getLabel(),
        s.getReason(),
        s.getKftcAction(),
        s.getContactName(),
        s.getContactPhone(),
        s.getContactEmail(),
        s.getExtraNote(),
        s.getRegistrantName(),
        s.isRegistrantKftc(),
        s.getHandover(),
        s.getConfirmedAt(),
        s.getConfirmedByName(),
        s.getCompletedAt(),
        s.getReviewers().stream()
            .map(
                r ->
                    new SuspensionReviewerResponse(r.getName(), r.getRoleLabel(), r.getStartedAt()))
            .toList(),
        s.getCreatedAt(),
        s.getUpdatedAt(),
        new SuspensionPermissions(
            accessPolicy.canManage(user, s.getBusiness()),
            accessPolicy.canEdit(user, s),
            accessPolicy.canDelete(user, s),
            review && reviewOpen && !s.isReviewer(user.email()),
            review));
  }

  // ---- 공통 ----

  Suspension load(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("SUSPENSION_NOT_FOUND", "일시중단을 찾을 수 없습니다."));
  }

  /** 사용자가 볼 수 있는 범위의 조건. 결제원 담당자는 전 기관(고른 기관이 있으면 그 기관), 기관 담당자는 자기 기관. */
  private Specification<Suspension> scope(
      CurrentUser user,
      Business business,
      Long organizationId,
      SuspensionPeriod period,
      LocalDateTime now) {
    if (accessPolicy.canManage(user, business)) {
      return SuspensionSpecifications.of(business, null, organizationId, period, now);
    }
    if (accessPolicy.isOrganizationMember(user, business)) {
      return SuspensionSpecifications.of(business, user.organizationCode(), null, period, now);
    }
    throw new AccessDeniedException(SuspensionAccessPolicy.VIEW_DENIED);
  }

  /** 결제원 담당자가 고른 기관. 그 업무에 참여하는 사용 중 참가기관이어야 한다. */
  private Organization loadOrganization(Long organizationId, Business business) {
    if (organizationId == null) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "SUSPENSION_ORGANIZATION_REQUIRED",
          "organizationId",
          "기관을 골라 주세요.");
    }
    Organization organization;
    try {
      organization = organizationService.getActive(organizationId);
    } catch (BusinessException e) {
      throw invalidOrganization(business);
    }
    if (organization.getType() != OrganizationType.FINANCE_ORGANIZATION) {
      throw invalidOrganization(business);
    }
    ensureParticipates(organization, business);
    return organization;
  }

  /** 기관 담당자의 자기 기관. 그 업무에 참여해야 한다. */
  private Organization ownOrganization(CurrentUser user, Business business) {
    Organization organization = organizationService.getByCode(user.organizationCode());
    ensureParticipates(organization, business);
    return organization;
  }

  private static void ensureParticipates(Organization organization, Business business) {
    if (!organization.participatesIn(business)) {
      throw invalidOrganization(business);
    }
  }

  private static BusinessException invalidOrganization(Business business) {
    return BusinessException.onField(
        HttpStatus.BAD_REQUEST,
        "SUSPENSION_ORGANIZATION_INVALID",
        "organizationId",
        "기관은 " + business.getLabel() + " 에 참여하는 사용 중 참가기관이어야 합니다.");
  }

  /** 양식 값을 저장할 모양으로. 글은 앞뒤 공백을 떼고 빈 글은 null 로 둔다. 종료는 시작보다 뒤여야 한다. */
  private static Suspension.Content contentOf(SuspensionSaveRequest body) {
    LocalDateTime start = body.startAt().truncatedTo(ChronoUnit.MINUTES);
    LocalDateTime end = body.endAt().truncatedTo(ChronoUnit.MINUTES);
    if (!end.isAfter(start)) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "SUSPENSION_PERIOD_INVALID", "endAt", "종료 일시는 시작 일시보다 뒤여야 합니다.");
    }
    return new Suspension.Content(
        body.business(),
        start,
        end,
        body.reason().trim(),
        blankToNull(body.kftcAction()),
        body.contactName().trim(),
        body.contactPhone().trim(),
        blankToNull(body.contactEmail()),
        blankToNull(body.extraNote()));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** 그 업무를 맡은 사용 중 결제원 담당자(업무·개발·운영·상황실)의 이메일. 알림을 일으킨 본인은 뺀다. */
  private Set<String> kftcRecipients(Business business, CurrentUser actor) {
    Set<String> kftcCodes =
        organizationService.listActive().stream()
            .filter(o -> o.type() == OrganizationType.KFTC)
            .map(OrganizationResponse::code)
            .collect(Collectors.toSet());
    return accountService.listActiveMembers(business).stream()
        .filter(m -> kftcCodes.contains(m.organizationCode()))
        .map(BusinessMember::email)
        .filter(email -> !email.equals(actor.email()))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String roleLabelOf(CurrentUser user) {
    return user.jobRole().describe(user.itRoles());
  }

  private void recordStatus(
      CurrentUser user,
      Suspension suspension,
      SuspensionStatus before,
      String what,
      String[] handoverChange) {
    FieldChanges changes =
        FieldChanges.compare().field("status", "상태", before, suspension.getStatus());
    if (handoverChange != null) {
      changes.value("handover", "조치 및 인수인계 사항", handoverChange[0], handoverChange[1]);
    }
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.SUSPENSION,
            suspension.getId(),
            suspension.getBusiness(),
            suspension.getOrganization().getName() + " 일시중단 " + what,
            changes.toList()));
  }

  private static void ensureNotCompleted(Suspension suspension) {
    if (suspension.getStatus() == SuspensionStatus.COMPLETED) {
      throw BusinessException.conflict("SUSPENSION_COMPLETED", "완료한 일시중단은 바꿀 수 없습니다.");
    }
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  /** 이력 비교용 값. 일시는 "시작~종료" 한 줄이다. */
  private record Snapshot(
      String business,
      String period,
      String reason,
      String kftcAction,
      String contact,
      String extraNote) {

    static final Snapshot EMPTY = new Snapshot(null, null, null, null, null, null);

    static Snapshot of(Suspension s) {
      String contact =
          s.getContactName()
              + " "
              + s.getContactPhone()
              + (s.getContactEmail() == null ? "" : " " + s.getContactEmail());
      return new Snapshot(
          s.getBusiness().getLabel(),
          SuspensionNotifier.periodText(s.getStartAt(), s.getEndAt()),
          s.getReason(),
          s.getKftcAction(),
          contact,
          s.getExtraNote());
    }

    /** {@code before} 에서 이것으로 바뀐 항목. {@code all} 이면 값이 있는 항목을 모두 남긴다(등록 이력). */
    List<com.connectlink.history.dto.FieldChange> diff(Snapshot before, boolean all) {
      FieldChanges changes = FieldChanges.compare();
      add(changes, all, "business", "대상 업무", before.business, business);
      add(changes, all, "period", "중단 일시", before.period, period);
      add(changes, all, "reason", "중단 이유", before.reason, reason);
      add(changes, all, "kftcAction", "결제원 조치사항", before.kftcAction, kftcAction);
      add(changes, all, "contact", "담당자 연락처", before.contact, contact);
      add(changes, all, "extraNote", "추가 사항", before.extraNote, extraNote);
      return changes.toList();
    }

    private static void add(
        FieldChanges changes,
        boolean all,
        String field,
        String label,
        Object before,
        Object after) {
      if (all) {
        if (after != null) {
          changes.value(field, label, null, after);
        }
      } else {
        changes.field(field, label, before, after);
      }
    }

    /** 바뀐 항목 이름을 "중단 일시, 중단 이유" 꼴로. */
    String changedSummary(Snapshot after) {
      List<String> what = new ArrayList<>();
      if (!Objects.equals(business, after.business)) what.add("대상 업무");
      if (!Objects.equals(period, after.period)) what.add("중단 일시");
      if (!Objects.equals(reason, after.reason)) what.add("중단 이유");
      if (!Objects.equals(kftcAction, after.kftcAction)) what.add("결제원 조치사항");
      if (!Objects.equals(contact, after.contact)) what.add("담당자 연락처");
      if (!Objects.equals(extraNote, after.extraNote)) what.add("추가 사항");
      return String.join(", ", what);
    }
  }
}
