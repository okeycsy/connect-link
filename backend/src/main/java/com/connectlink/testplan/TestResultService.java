package com.connectlink.testplan;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.testplan.dto.TestFeedbackRequest;
import com.connectlink.testplan.dto.TestItemResultResponse;
import com.connectlink.testplan.dto.TestResultSaveRequest;
import com.connectlink.testplan.dto.TestTargetResultsResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 테스트 결과. 대상 기관 담당자의 요구사항별 결과 등록과 결제원 담당자의 의견(미니 수행률·특이사항)을 맡는다. 결과나 의견이 바뀌면 수행률과 수행 완료를 다시
 * 계산하고, 기관별 변경 이력(TEST_PLAN_TARGET)을 남긴다.
 */
@Service
@Transactional
public class TestResultService {

  private final TestPlanService planService;
  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;
  private final ChangeHistoryService changeHistoryService;
  private final TestPlanNotifier notifier;
  private final Clock clock;

  public TestResultService(
      TestPlanService planService,
      TestPlanRepository repository,
      TestPlanAccessPolicy accessPolicy,
      ChangeHistoryService changeHistoryService,
      TestPlanNotifier notifier,
      Clock clock) {
    this.planService = planService;
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.changeHistoryService = changeHistoryService;
    this.notifier = notifier;
    this.clock = clock;
  }

  /** 대상 기관 한 곳의 요구사항별 결과. 결제원 담당자는 어느 기관이든, 기관 담당자는 자기 기관만 본다. */
  @Transactional(readOnly = true)
  public TestTargetResultsResponse get(CurrentUser user, Long planId, Long organizationId) {
    TestPlanTarget target = loadTarget(user, planId, organizationId);
    accessPolicy.ensureCanViewTarget(user, target);
    return toResponse(user, target);
  }

  /**
   * 기관 담당자가 자기 기관 결과를 저장한다. 바뀐 행만 등록 일시가 바뀌고, 바뀐 것이 있으면 테스트의 알림 대상 결제원 담당자에게 매터모스트로 알린다. 오류면
   * 특이사항(오류내용)을 꼭 써야 한다.
   */
  public TestTargetResultsResponse report(
      CurrentUser user, Long planId, Long organizationId, TestResultSaveRequest body) {
    TestPlanTarget target = loadTarget(user, planId, organizationId);
    accessPolicy.ensureCanReport(user, target);
    TestPlan plan = target.getPlan();
    Set<Long> seen = new HashSet<>();
    for (int i = 0; i < body.results().size(); i++) {
      TestResultSaveRequest.Entry entry = body.results().get(i);
      if (plan.findItem(entry.itemId()).isEmpty() || !seen.add(entry.itemId())) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "TEST_ITEM_INVALID",
            "results[" + i + "].itemId",
            "이 테스트의 요구사항이 아닙니다.");
      }
      if (entry.result() == TestResult.FAILED && blankToNull(entry.note()) == null) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "TEST_RESULT_NOTE_REQUIRED",
            "results[" + i + "].note",
            "오류면 특이사항에 오류내용을 써 주세요.");
      }
    }
    LocalDateTime now = now();
    FieldChanges changes = FieldChanges.compare();
    List<String> summary = new ArrayList<>();
    for (TestResultSaveRequest.Entry entry : body.results()) {
      TestItem item = plan.findItem(entry.itemId()).orElseThrow();
      String note = blankToNull(entry.note());
      TestItemResult existing = target.findResult(item.getId()).orElse(null);
      String before =
          existing == null ? null : resultText(existing.getResult(), existing.getNote());
      boolean changed;
      if (existing == null) {
        target.addResult(
            new TestItemResult(
                item,
                target,
                entry.result(),
                note,
                user.email(),
                user.name(),
                user.jobTitle(),
                now));
        changed = true;
      } else {
        changed =
            existing.report(entry.result(), note, user.email(), user.name(), user.jobTitle(), now);
      }
      if (changed) {
        changes.field(
            "item" + item.getId(),
            item.getNumber() + " " + item.getTitle(),
            before,
            resultText(entry.result(), note));
        summary.add(item.getNumber() + " " + entry.result().getLabel());
      }
    }
    if (summary.isEmpty()) {
      return toResponse(user, target);
    }
    target.reported(now);
    TestProgress.recalculate(plan, now);
    repository.flush();
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.TEST_PLAN_TARGET,
            target.getId(),
            plan.getBusiness(),
            target.getOrganization().getName() + " 결과 등록",
            changes.toList()));
    Set<String> recipients = new LinkedHashSet<>();
    plan.getRecipients().forEach(r -> recipients.add(r.getEmail()));
    recipients.remove(user.email());
    notifier.reported(plan, target, recipients, user.name(), String.join(", ", summary));
    return toResponse(user, target);
  }

  /**
   * 결제원 담당자가 기관 결과 하나에 의견(미니 수행률·특이사항)을 남긴다. 둘 다 비우면 의견을 지운다. 기관이 아직 결과를 등록하지 않은 요구사항에는 남기지 않는다.
   * 결과를 등록한 기관 담당자에게 알림 센터로 알린다.
   */
  public TestTargetResultsResponse feedback(
      CurrentUser user, Long planId, Long organizationId, Long itemId, TestFeedbackRequest body) {
    TestPlanTarget target = loadTarget(user, planId, organizationId);
    TestPlan plan = target.getPlan();
    accessPolicy.ensureCanFeedback(user, plan);
    TestItem item =
        plan.findItem(itemId)
            .orElseThrow(
                () -> BusinessException.notFound("TEST_ITEM_NOT_FOUND", "요구사항을 찾을 수 없습니다."));
    TestItemResult result =
        target
            .findResult(itemId)
            .orElseThrow(
                () ->
                    BusinessException.conflict(
                        "TEST_RESULT_NOT_REPORTED", "기관이 아직 결과를 등록하지 않은 요구사항입니다."));
    String note = blankToNull(body.note());
    String before = feedbackText(result.getFeedbackRate(), result.getFeedbackNote());
    String after = feedbackText(body.rate(), note);
    if (Objects.equals(before, after) && (after == null || result.hasCurrentFeedback())) {
      // 같은 의견을 다시 저장했으면 아무것도 바꾸지 않는다. 지난 의견을 그대로 다시 저장하면 다시 확인한 것으로 본다.
      return toResponse(user, target);
    }
    LocalDateTime now = now();
    result.giveFeedback(body.rate(), note, user.email(), user.name(), user.jobTitle(), now);
    TestProgress.recalculate(plan, now);
    repository.flush();
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.TEST_PLAN_TARGET,
            target.getId(),
            plan.getBusiness(),
            target.getOrganization().getName() + " 결제원 의견",
            FieldChanges.compare()
                .value(
                    "feedback" + item.getId(),
                    item.getNumber() + " " + item.getTitle() + " 결제원 의견",
                    before,
                    after)
                .toList()));
    if (after != null && !result.getReportedByEmail().equals(user.email())) {
      notifier.feedback(plan, result, user.name());
    }
    return toResponse(user, target);
  }

  // ---- 응답 ----

  private TestTargetResultsResponse toResponse(CurrentUser user, TestPlanTarget target) {
    TestPlan plan = target.getPlan();
    List<TestItem> items = plan.getItems();
    List<TestItemResultResponse> rows = new ArrayList<>();
    for (TestItem item : items) {
      TestItemResult r = target.findResult(item.getId()).orElse(null);
      rows.add(
          r == null
              ? new TestItemResultResponse(
                  item.getId(),
                  item.getNumber(),
                  item.getTitle(),
                  item.getDescription(),
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  false,
                  0,
                  false)
              : new TestItemResultResponse(
                  item.getId(),
                  item.getNumber(),
                  item.getTitle(),
                  item.getDescription(),
                  r.getResult(),
                  r.getResult().getLabel(),
                  r.getNote(),
                  r.getReportedByName(),
                  r.getReportedByJobTitle(),
                  r.getReportedAt(),
                  r.getFeedbackRate(),
                  r.getFeedbackNote(),
                  r.getFeedbackByName(),
                  r.getFeedbackByJobTitle(),
                  r.getFeedbackAt(),
                  r.isFeedbackOutdated(),
                  r.effectiveRate(),
                  r.isCompleted()));
    }
    return new TestTargetResultsResponse(
        plan.getId(),
        TestPlanService.toTargetSummary(target, items),
        rows,
        accessPolicy.canReport(user, target),
        accessPolicy.canFeedback(user, plan));
  }

  // ---- 공통 ----

  /** 테스트를 볼 수 있는지 먼저 확인한 뒤 대상 기관을 찾는다(볼 수 없는 테스트의 대상 여부를 드러내지 않는다). */
  private TestPlanTarget loadTarget(CurrentUser user, Long planId, Long organizationId) {
    TestPlan plan = planService.load(planId);
    accessPolicy.ensureCanView(user, plan);
    return plan.findTarget(organizationId)
        .orElseThrow(
            () -> BusinessException.notFound("TEST_TARGET_NOT_FOUND", "이 테스트의 대상 기관이 아닙니다."));
  }

  /** 이력 값: "정상", "오류 · 오류내용". */
  private static String resultText(TestResult result, String note) {
    return note == null ? result.getLabel() : result.getLabel() + " · " + note;
  }

  /** 이력 값: "80% · 003 전문 예외". 의견이 없으면 null. */
  private static String feedbackText(Integer rate, String note) {
    if (rate == null && note == null) {
      return null;
    }
    if (rate == null) {
      return note;
    }
    return note == null ? rate + "%" : rate + "% · " + note;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }
}
