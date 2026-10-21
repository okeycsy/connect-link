package com.connectlink.testplan;

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
import com.connectlink.history.dto.FieldChange;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import com.connectlink.organization.dto.OrganizationResponse;
import com.connectlink.testplan.dto.TestItemRequest;
import com.connectlink.testplan.dto.TestItemResponse;
import com.connectlink.testplan.dto.TestPlanCountsResponse;
import com.connectlink.testplan.dto.TestPlanDetailResponse;
import com.connectlink.testplan.dto.TestPlanOrganizationResponse;
import com.connectlink.testplan.dto.TestPlanPermissions;
import com.connectlink.testplan.dto.TestPlanRecipientResponse;
import com.connectlink.testplan.dto.TestPlanSaveRequest;
import com.connectlink.testplan.dto.TestPlanSummaryResponse;
import com.connectlink.testplan.dto.TestTargetSummaryResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 테스트. 결제원 담당자의 만들기·고치기·지우기와 알림 대상 설정, 진행 중·수행 완료 목록과 상세를 맡는다. 기관 결과 등록과 결제원 의견은 {@link
 * TestResultService} 가 한다. 누가 무엇을 하는지는 {@link TestPlanAccessPolicy} 가 정한다.
 */
@Service
@Transactional
public class TestPlanService {

  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;
  private final OrganizationService organizationService;
  private final AccountService accountService;
  private final AttachmentService attachmentService;
  private final ChangeHistoryService changeHistoryService;
  private final TestPlanNotifier notifier;
  private final Clock clock;

  public TestPlanService(
      TestPlanRepository repository,
      TestPlanAccessPolicy accessPolicy,
      OrganizationService organizationService,
      AccountService accountService,
      AttachmentService attachmentService,
      ChangeHistoryService changeHistoryService,
      TestPlanNotifier notifier,
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

  /** 한 업무의 기관 테스트 중 한 진행 구분(없으면 전부). 결제원 담당자는 전부, 기관 담당자는 자기 기관이 대상인 것만 본다. */
  @Transactional(readOnly = true)
  public PageResponse<TestPlanSummaryResponse> list(
      CurrentUser user, Business business, TestPlanState state, Pageable pageable) {
    Pageable sorted =
        PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(), TestPlanSpecifications.sortOf(state));
    Page<TestPlan> page =
        repository.findAll(
            TestPlanSpecifications.of(business, scopeCode(user, business), state), sorted);
    LocalDate today = today();
    return PageResponse.from(page, p -> toSummary(user, p, today));
  }

  @Transactional(readOnly = true)
  public TestPlanCountsResponse counts(CurrentUser user, Business business) {
    String code = scopeCode(user, business);
    return new TestPlanCountsResponse(
        repository.count(TestPlanSpecifications.of(business, code, TestPlanState.IN_PROGRESS)),
        repository.count(TestPlanSpecifications.of(business, code, TestPlanState.COMPLETED)));
  }

  @Transactional(readOnly = true)
  public TestPlanDetailResponse get(CurrentUser user, Long id) {
    TestPlan plan = load(id);
    accessPolicy.ensureCanView(user, plan);
    return toDetail(user, plan);
  }

  /** 대상 기관 후보. 그 업무에 참여하는 사용 중 참가기관(이름순)이다. 결제원 담당자만 쓴다. */
  @Transactional(readOnly = true)
  public List<TestPlanOrganizationResponse> organizations(CurrentUser user, Business business) {
    accessPolicy.ensureCanManage(user, business);
    return organizationService.listActive().stream()
        .filter(o -> o.type() == OrganizationType.FINANCE_ORGANIZATION)
        .filter(o -> o.businesses().contains(business))
        .map(
            o ->
                new TestPlanOrganizationResponse(
                    o.id(), o.code(), o.name(), o.sector(), o.sectorLabel()))
        .toList();
  }

  // ---- 만들기·고치기·지우기 ----

  /** 만든다. 바로 대상 기관에 열리고 대상 기관 담당자에게 알림·메일이 간다. 만든 사람이 결과 등록 알림 대상이 된다. */
  public TestPlanDetailResponse create(CurrentUser user, TestPlanSaveRequest body) {
    accessPolicy.ensureCanManage(user, body.business());
    validate(body);
    TestPlan plan = TestPlan.create(body.business(), contentOf(body), user.email(), user.name());
    // 새 테스트라 요구사항 id 는 무시하고 모두 새로 넣는다.
    plan.replaceItems(
        itemDrafts(body.items()).stream()
            .map(d -> new TestPlan.ItemDraft(null, d.title(), d.description()))
            .toList());
    for (Organization organization : loadTargets(body.organizationIds(), body.business())) {
      plan.addTarget(organization);
    }
    plan.replaceRecipients(ownerAsRecipient(user, body.business()));
    TestProgress.recalculate(plan, now());
    repository.saveAndFlush(plan);
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.created(
            HistoryTargetType.TEST_PLAN,
            plan.getId(),
            plan.getBusiness(),
            plan.getTitle() + " 기관 테스트 등록",
            Snapshot.of(plan).diff(Snapshot.EMPTY, true)));
    notifier.opened(plan, organizationEmails(plan.getBusiness(), targetCodes(plan.getTargets())));
    return toDetail(user, plan);
  }

  /**
   * 내용·요구사항·대상 기관을 고친다. 결과가 있는 요구사항·기관은 뺄 수 없다. 새로 더한 대상 기관 담당자에게 알림·메일이 간다. 요구사항이나 기관이 늘면 수행 완료가
   * 풀릴 수 있다.
   */
  public TestPlanDetailResponse update(CurrentUser user, Long id, TestPlanSaveRequest body) {
    TestPlan plan = load(id);
    accessPolicy.ensureCanManage(user, plan.getBusiness());
    if (body.business() != plan.getBusiness()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "TEST_PLAN_BUSINESS_FIXED", "business", "업무는 바꿀 수 없습니다.");
    }
    validate(body);
    Snapshot before = Snapshot.of(plan);
    plan.change(contentOf(body));
    applyItems(plan, body.items());
    Set<String> added = applyTargets(plan, body.organizationIds());
    TestProgress.recalculate(plan, now());
    repository.flush();
    List<FieldChange> changes = Snapshot.of(plan).diff(before, false);
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.TEST_PLAN,
            plan.getId(),
            plan.getBusiness(),
            plan.getTitle() + " 기관 테스트 수정",
            changes));
    if (!added.isEmpty()) {
      notifier.opened(plan, organizationEmails(plan.getBusiness(), added));
    }
    return toDetail(user, plan);
  }

  /** 지운다. 기관 결과와 가이드·테스트데이터 양식·기관 테스트데이터 첨부도 함께 지운다. */
  public void delete(CurrentUser user, Long id) {
    TestPlan plan = load(id);
    accessPolicy.ensureCanManage(user, plan.getBusiness());
    attachmentService.deleteAllOf(AttachmentOwnerType.TEST_PLAN, plan.getId());
    attachmentService.deleteAllOf(AttachmentOwnerType.TEST_DATA_FORM, plan.getId());
    plan.getTargets()
        .forEach(t -> attachmentService.deleteAllOf(AttachmentOwnerType.TEST_DATA, t.getId()));
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.deleted(
            HistoryTargetType.TEST_PLAN,
            plan.getId(),
            plan.getBusiness(),
            plan.getTitle() + " 기관 테스트 삭제",
            FieldChanges.compare()
                .value("title", "개발 건", plan.getTitle(), null)
                .value("subBusiness", "단위업무", plan.getSubBusiness(), null)
                .toList()));
    // 결과가 요구사항을 가리키므로 결과부터 지운다.
    plan.getTargets().forEach(TestPlanTarget::clearResults);
    repository.flush();
    repository.delete(plan);
  }

  // ---- 알림 대상 ----

  /** 알림 대상 후보(그 업무를 맡은 사용 중 결제원 계정)와 지금 고른 사람. 결제원 담당자만 쓴다. */
  @Transactional(readOnly = true)
  public List<TestPlanRecipientResponse> recipients(CurrentUser user, Long id) {
    TestPlan plan = load(id);
    accessPolicy.ensureCanManage(user, plan.getBusiness());
    return recipientsOf(plan);
  }

  /** 알림 대상을 통째로 바꾼다. 그 업무를 맡은 사용 중 결제원 계정만 고를 수 있다. */
  public List<TestPlanRecipientResponse> replaceRecipients(
      CurrentUser user, Long id, List<Long> accountIds) {
    TestPlan plan = load(id);
    accessPolicy.ensureCanManage(user, plan.getBusiness());
    Map<Long, BusinessMember> candidates = kftcMembers(plan.getBusiness());
    List<String> before = plan.getRecipients().stream().map(TestPlanRecipient::getName).toList();
    List<TestPlanRecipient.Draft> drafts = new ArrayList<>();
    for (Long accountId : new LinkedHashSet<>(accountIds)) {
      BusinessMember member = candidates.get(accountId);
      if (member == null) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "TEST_RECIPIENT_INVALID",
            "accountIds",
            plan.getBusiness().getLabel() + " 을(를) 맡은 결제원 담당자만 알림 대상으로 고를 수 있습니다.");
      }
      drafts.add(new TestPlanRecipient.Draft(member.accountId(), member.email(), member.name()));
    }
    plan.replaceRecipients(drafts);
    repository.flush();
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.TEST_PLAN,
            plan.getId(),
            plan.getBusiness(),
            plan.getTitle() + " 알림 대상 변경",
            FieldChanges.compare()
                .field(
                    "recipients",
                    "결과 등록 알림 대상",
                    FieldChanges.join(before),
                    FieldChanges.join(
                        plan.getRecipients().stream().map(TestPlanRecipient::getName).toList()))
                .toList()));
    return recipientsOf(plan);
  }

  private List<TestPlanRecipientResponse> recipientsOf(TestPlan plan) {
    Map<Long, BusinessMember> candidates = kftcMembers(plan.getBusiness());
    List<TestPlanRecipientResponse> rows = new ArrayList<>();
    for (BusinessMember m : candidates.values()) {
      rows.add(
          new TestPlanRecipientResponse(
              m.accountId(),
              m.name(),
              m.email(),
              m.jobRoleLabel(),
              m.department(),
              m.team(),
              plan.isRecipient(m.email()),
              true));
    }
    Set<String> candidateEmails =
        candidates.values().stream().map(BusinessMember::email).collect(Collectors.toSet());
    for (TestPlanRecipient r : plan.getRecipients()) {
      if (!candidateEmails.contains(r.getEmail())) {
        rows.add(
            new TestPlanRecipientResponse(
                r.getAccountId(), r.getName(), r.getEmail(), null, null, null, true, false));
      }
    }
    return rows;
  }

  // ---- 응답 ----

  TestPlanSummaryResponse toSummary(CurrentUser user, TestPlan plan, LocalDate today) {
    boolean manage = accessPolicy.canManage(user, plan.getBusiness());
    Optional<TestPlanTarget> own =
        manage ? Optional.empty() : plan.findTargetByCode(user.organizationCode());
    List<TestPlanTarget> targets = plan.getTargets();
    int itemCount = plan.getItems().size();
    return new TestPlanSummaryResponse(
        plan.getId(),
        plan.getBusiness(),
        plan.getBusiness().getLabel(),
        plan.getSubBusiness(),
        plan.getTitle(),
        plan.getStartDate(),
        plan.getEndDate(),
        plan.getState(),
        plan.getState().getLabel(),
        plan.isOverdue(today),
        manage ? plan.getProgressRate() : own.map(TestPlanTarget::getProgressRate).orElse(0),
        itemCount,
        manage ? targets.size() : null,
        manage ? (int) targets.stream().filter(t -> isCompleted(t, itemCount)).count() : null,
        own.map(TestPlanTarget::getCompletedCount).orElse(null),
        own.map(TestPlanTarget::getLastReportedAt).orElse(null),
        plan.getOwnerName());
  }

  TestPlanDetailResponse toDetail(CurrentUser user, TestPlan plan) {
    boolean manage = accessPolicy.canManage(user, plan.getBusiness());
    List<TestItem> items = plan.getItems();
    List<TestPlanTarget> targets =
        manage
            ? plan.getTargets()
            : plan.findTargetByCode(user.organizationCode()).stream().toList();
    Optional<TestPlanTarget> own = manage ? Optional.empty() : targets.stream().findFirst();
    return new TestPlanDetailResponse(
        plan.getId(),
        plan.getBusiness(),
        plan.getBusiness().getSlug(),
        plan.getBusiness().getLabel(),
        plan.getSubBusiness(),
        plan.getTitle(),
        plan.getDescription(),
        plan.getStartDate(),
        plan.getEndDate(),
        plan.getState(),
        plan.getState().getLabel(),
        plan.isOverdue(today()),
        manage ? plan.getProgressRate() : own.map(TestPlanTarget::getProgressRate).orElse(0),
        plan.getCompletedAt(),
        plan.getOwnerName(),
        items.stream()
            .map(
                i ->
                    new TestItemResponse(
                        i.getId(), i.getNumber(), i.getTitle(), i.getDescription()))
            .toList(),
        targets.stream().map(t -> toTargetSummary(t, items)).toList(),
        own.map(t -> t.getOrganization().getId()).orElse(null),
        manage ? plan.getRecipients().stream().map(TestPlanRecipient::getName).toList() : List.of(),
        plan.getCreatedAt(),
        plan.getUpdatedAt(),
        new TestPlanPermissions(
            manage,
            own.map(t -> accessPolicy.canReport(user, t)).orElse(false),
            accessPolicy.canFeedback(user, plan)));
  }

  /** 대상 기관 한 곳의 현황. 결과 서비스와 함께 쓴다. */
  static TestTargetSummaryResponse toTargetSummary(TestPlanTarget target, List<TestItem> items) {
    Map<TestResult, Integer> counts = TestProgress.countByResult(target, items);
    int reported = counts.values().stream().mapToInt(Integer::intValue).sum();
    int awaiting = 0;
    for (TestItem item : items) {
      TestItemResult r = target.findResult(item.getId()).orElse(null);
      if (r != null && needsReview(r)) {
        awaiting++;
      }
    }
    return new TestTargetSummaryResponse(
        target.getId(),
        target.getOrganization().getId(),
        target.getOrganization().getCode(),
        target.getOrganization().getName(),
        target.getProgressRate(),
        target.getCompletedCount(),
        items.size(),
        counts.get(TestResult.PASSED),
        counts.get(TestResult.FAILED),
        counts.get(TestResult.IN_PROGRESS),
        counts.get(TestResult.NOT_STARTED),
        items.size() - reported,
        awaiting,
        isCompleted(target, items.size()),
        target.getLastReportedAt(),
        target.getTestDataStatus(),
        target.getTestDataStatus().getLabel());
  }

  /** 결제원이 확인할 결과인지: 오류이거나, 정상인데 결제원 의견이 없거나 지난 의견. 홈 카드의 집계와 같은 기준이다. */
  static boolean needsReview(TestItemResult result) {
    return result.getResult() == TestResult.FAILED
        || (result.getResult() == TestResult.PASSED && !result.hasCurrentFeedback());
  }

  private static boolean isCompleted(TestPlanTarget target, int itemCount) {
    return itemCount > 0 && target.getCompletedCount() == itemCount;
  }

  // ---- 공통 ----

  TestPlan load(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("TEST_PLAN_NOT_FOUND", "기관 테스트를 찾을 수 없습니다."));
  }

  /** 목록 범위. 결제원 담당자는 null(전부), 기관 담당자는 자기 기관 코드. 그 밖은 거부. */
  private String scopeCode(CurrentUser user, Business business) {
    if (accessPolicy.canManage(user, business)) {
      return null;
    }
    if (accessPolicy.isOrganizationMember(user, business)) {
      return user.organizationCode();
    }
    throw new AccessDeniedException(TestPlanAccessPolicy.VIEW_DENIED);
  }

  private static void validate(TestPlanSaveRequest body) {
    if (body.endDate().isBefore(body.startDate())) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "TEST_PLAN_PERIOD_INVALID",
          "endDate",
          "수행 종료일은 시작일과 같거나 뒤여야 합니다.");
    }
    if (body.items().isEmpty()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "TEST_PLAN_ITEMS_REQUIRED", "items", "요구사항을 하나 이상 넣어 주세요.");
    }
    if (body.organizationIds().isEmpty()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "TEST_PLAN_TARGETS_REQUIRED",
          "organizationIds",
          "대상 기관을 하나 이상 골라 주세요.");
    }
    if (new HashSet<>(body.organizationIds()).size() != body.organizationIds().size()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "TEST_TARGET_DUPLICATE", "organizationIds", "같은 기관을 두 번 골랐습니다.");
    }
  }

  private static TestPlan.Content contentOf(TestPlanSaveRequest body) {
    return new TestPlan.Content(
        body.subBusiness().trim(),
        body.title().trim(),
        blankToNull(body.description()),
        body.startDate(),
        body.endDate());
  }

  private static List<TestPlan.ItemDraft> itemDrafts(List<TestItemRequest> items) {
    return items.stream()
        .map(i -> new TestPlan.ItemDraft(i.id(), i.title().trim(), blankToNull(i.description())))
        .toList();
  }

  /** 요구사항을 요청 목록대로 맞춘다. 이 테스트의 요구사항이 아닌 id 는 거절하고, 결과가 있는 요구사항은 뺄 수 없다. */
  private void applyItems(TestPlan plan, List<TestItemRequest> items) {
    Set<Long> keptIds = new HashSet<>();
    for (int i = 0; i < items.size(); i++) {
      Long itemId = items.get(i).id();
      if (itemId == null) {
        continue;
      }
      if (plan.findItem(itemId).isEmpty() || !keptIds.add(itemId)) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST, "TEST_ITEM_INVALID", "items[" + i + "]", "이 테스트의 요구사항이 아닙니다.");
      }
    }
    for (TestItem item : plan.getItems()) {
      if (!keptIds.contains(item.getId())
          && plan.getTargets().stream().anyMatch(t -> t.hasResult(item.getId()))) {
        throw BusinessException.conflict(
            "TEST_ITEM_HAS_RESULTS",
            item.getNumber() + " " + item.getTitle() + " 은(는) 기관이 결과를 등록해서 뺄 수 없습니다.");
      }
    }
    plan.replaceItems(itemDrafts(items));
  }

  /**
   * 대상 기관을 요청 목록대로 맞춘다. 결과나 테스트데이터를 등록한 기관은 뺄 수 없다.
   *
   * @return 새로 더한 기관 코드
   */
  private Set<String> applyTargets(TestPlan plan, List<Long> organizationIds) {
    List<Organization> organizations = loadTargets(organizationIds, plan.getBusiness());
    Set<Long> ids = organizations.stream().map(Organization::getId).collect(Collectors.toSet());
    for (TestPlanTarget target : plan.getTargets()) {
      if (!ids.contains(target.getOrganization().getId())) {
        if (!target.getResults().isEmpty()) {
          throw BusinessException.conflict(
              "TEST_TARGET_HAS_RESULTS",
              target.getOrganization().getName() + " 은(는) 결과를 등록해서 대상에서 뺄 수 없습니다.");
        }
        if (target.getTestDataStatus() != TestDataStatus.NOT_CREATED
            || target.getTestDataNote() != null
            || !attachmentService
                .listOf(AttachmentOwnerType.TEST_DATA, List.of(target.getId()))
                .isEmpty()) {
          throw BusinessException.conflict(
              "TEST_TARGET_HAS_TEST_DATA",
              target.getOrganization().getName() + " 은(는) 테스트데이터를 등록해서 대상에서 뺄 수 없습니다.");
        }
        plan.removeTarget(target);
      }
    }
    Set<String> added = new LinkedHashSet<>();
    for (Organization organization : organizations) {
      if (plan.findTarget(organization.getId()).isEmpty()) {
        plan.addTarget(organization);
        added.add(organization.getCode());
      }
    }
    return added;
  }

  /** 대상 기관은 그 업무에 참여하는 사용 중 참가기관이어야 한다. */
  private List<Organization> loadTargets(List<Long> organizationIds, Business business) {
    List<Organization> organizations = new ArrayList<>();
    for (Long organizationId : organizationIds) {
      Organization organization;
      try {
        organization = organizationService.getActive(organizationId);
      } catch (BusinessException e) {
        throw invalidTarget(business);
      }
      if (organization.getType() != OrganizationType.FINANCE_ORGANIZATION
          || !organization.participatesIn(business)) {
        throw invalidTarget(business);
      }
      organizations.add(organization);
    }
    return organizations;
  }

  private static BusinessException invalidTarget(Business business) {
    return BusinessException.onField(
        HttpStatus.BAD_REQUEST,
        "TEST_TARGET_INVALID",
        "organizationIds",
        "대상 기관은 " + business.getLabel() + " 에 참여하는 사용 중 참가기관이어야 합니다.");
  }

  /** 만든 사람을 첫 알림 대상으로. 관리자처럼 계정 목록에서 찾지 못해도 이메일로 넣는다. */
  private List<TestPlanRecipient.Draft> ownerAsRecipient(CurrentUser user, Business business) {
    Long accountId =
        kftcMembers(business).values().stream()
            .filter(m -> m.email().equals(user.email()))
            .map(BusinessMember::accountId)
            .findFirst()
            .orElse(null);
    return List.of(new TestPlanRecipient.Draft(accountId, user.email(), user.name()));
  }

  /** 그 업무를 맡은 사용 중 결제원 계정(이름순), 계정 ID 로 찾는다. */
  private Map<Long, BusinessMember> kftcMembers(Business business) {
    Set<String> kftcCodes = kftcCodes();
    return accountService.listActiveMembers(business).stream()
        .filter(m -> kftcCodes.contains(m.organizationCode()))
        .collect(
            Collectors.toMap(BusinessMember::accountId, m -> m, (a, b) -> a, LinkedHashMap::new));
  }

  private Set<String> kftcCodes() {
    return organizationService.listActive().stream()
        .filter(o -> o.type() == OrganizationType.KFTC)
        .map(OrganizationResponse::code)
        .collect(Collectors.toSet());
  }

  /** 그 기관들의 그 업무 담당자(사용 중 계정) 이메일. */
  private Set<String> organizationEmails(Business business, Set<String> organizationCodes) {
    return accountService.listActiveMembers(business).stream()
        .filter(m -> organizationCodes.contains(m.organizationCode()))
        .map(BusinessMember::email)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static Set<String> targetCodes(List<TestPlanTarget> targets) {
    return targets.stream()
        .map(t -> t.getOrganization().getCode())
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  LocalDate today() {
    return LocalDate.now(clock);
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }

  /** 이력 비교용 값. 요구사항은 "001 요구사항" 을 줄마다, 대상 기관은 이름을 쉼표로 잇는다. */
  private record Snapshot(
      String subBusiness,
      String title,
      String description,
      String period,
      String items,
      String targets) {

    static final Snapshot EMPTY = new Snapshot(null, null, null, null, null, null);

    static Snapshot of(TestPlan plan) {
      return new Snapshot(
          plan.getSubBusiness(),
          plan.getTitle(),
          plan.getDescription(),
          TestPlanNotifier.periodText(plan),
          plan.getItems().stream()
              .map(i -> i.getNumber() + " " + i.getTitle())
              .collect(Collectors.joining("\n")),
          FieldChanges.join(
              plan.getTargets().stream().map(t -> t.getOrganization().getName()).toList()));
    }

    /** {@code before} 에서 이것으로 바뀐 항목. {@code all} 이면 값이 있는 항목을 모두 남긴다(등록 이력). */
    List<FieldChange> diff(Snapshot before, boolean all) {
      FieldChanges changes = FieldChanges.compare();
      add(changes, all, "subBusiness", "단위업무", before.subBusiness, subBusiness);
      add(changes, all, "title", "개발 건", before.title, title);
      add(changes, all, "description", "테스트 내용", before.description, description);
      add(changes, all, "period", "수행 기간", before.period, period);
      add(changes, all, "items", "요구사항", before.items, items);
      add(changes, all, "targets", "대상 기관", before.targets, targets);
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
  }
}
