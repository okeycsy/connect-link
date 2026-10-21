package com.connectlink.testplan;

import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.attachment.AttachmentService;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.testplan.dto.TestDataResponse;
import com.connectlink.testplan.dto.TestDataSaveRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 테스트 전 테스트데이터 준비. 대상 기관 담당자가 자기 기관의 테스트데이터 생성 여부(미생성·생성 완료)와 메모를 적고, 만든 테스트데이터 파일을 첨부
 * (TEST_DATA)로 올린다. 결제원 담당자는 전 기관 상태를 본다. 바뀌면 기관별 변경 이력(TEST_PLAN_TARGET)을 남기고, 생성 완료가 되면 알림 대상 결제원
 * 담당자에게 매터모스트로 알린다. 테스트데이터가 없어도 결과 등록은 막지 않는다.
 */
@Service
@Transactional
public class TestDataService {

  private final TestPlanService planService;
  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;
  private final AttachmentService attachmentService;
  private final ChangeHistoryService changeHistoryService;
  private final TestPlanNotifier notifier;
  private final Clock clock;

  public TestDataService(
      TestPlanService planService,
      TestPlanRepository repository,
      TestPlanAccessPolicy accessPolicy,
      AttachmentService attachmentService,
      ChangeHistoryService changeHistoryService,
      TestPlanNotifier notifier,
      Clock clock) {
    this.planService = planService;
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.attachmentService = attachmentService;
    this.changeHistoryService = changeHistoryService;
    this.notifier = notifier;
    this.clock = clock;
  }

  /** 대상 기관 한 곳의 테스트데이터 상태. 결제원 담당자는 어느 기관이든, 기관 담당자는 자기 기관만 본다. */
  @Transactional(readOnly = true)
  public TestDataResponse get(CurrentUser user, Long planId, Long organizationId) {
    TestPlanTarget target = loadTarget(user, planId, organizationId);
    accessPolicy.ensureCanViewTarget(user, target);
    return toResponse(user, target);
  }

  /** 그 기관 담당자가 자기 기관의 테스트데이터 생성 여부와 메모를 저장한다. */
  public TestDataResponse save(
      CurrentUser user, Long planId, Long organizationId, TestDataSaveRequest body) {
    TestPlanTarget target = loadTarget(user, planId, organizationId);
    accessPolicy.ensureCanReport(user, target);
    if (!target.getPlan().isTestDataRequested()) {
      throw BusinessException.conflict("TEST_DATA_NOT_REQUESTED", "테스트데이터를 요청하지 않은 기관 테스트입니다.");
    }
    TestDataStatus beforeStatus = target.getTestDataStatus();
    String beforeNote = target.getTestDataNote();
    String note = blankToNull(body.note());
    if (!target.reportTestData(body.status(), note, user.email(), user.name(), now())) {
      return toResponse(user, target);
    }
    repository.flush();
    TestPlan plan = target.getPlan();
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.TEST_PLAN_TARGET,
            target.getId(),
            plan.getBusiness(),
            target.getOrganization().getName() + " 테스트데이터",
            FieldChanges.compare()
                .field("testDataStatus", "테스트데이터 생성 여부", beforeStatus, body.status())
                .field("testDataNote", "테스트데이터 메모", beforeNote, note)
                .toList()));
    if (beforeStatus != TestDataStatus.CREATED && body.status() == TestDataStatus.CREATED) {
      Set<String> recipients = new LinkedHashSet<>();
      plan.getRecipients().forEach(r -> recipients.add(r.getEmail()));
      recipients.remove(user.email());
      notifier.testDataCreated(plan, target, recipients, user.name(), fileCount(target));
    }
    return toResponse(user, target);
  }

  /** 대상 기관 행 ID → 올린 테스트데이터 파일 수. 기관별 현황이 한 번에 읽는다. 권한은 부르는 쪽이 이미 확인했다. */
  @Transactional(readOnly = true)
  public Map<Long, Integer> fileCounts(Collection<Long> targetIds) {
    Map<Long, Integer> counts = new HashMap<>();
    attachmentService
        .listOf(AttachmentOwnerType.TEST_DATA, targetIds)
        .forEach((id, files) -> counts.put(id, files.size()));
    return counts;
  }

  private int fileCount(TestPlanTarget target) {
    return fileCounts(List.of(target.getId())).getOrDefault(target.getId(), 0);
  }

  private TestDataResponse toResponse(CurrentUser user, TestPlanTarget target) {
    return new TestDataResponse(
        target.getPlan().getId(),
        target.getId(),
        target.getOrganization().getId(),
        target.getOrganization().getName(),
        target.getTestDataStatus(),
        target.getTestDataStatus().getLabel(),
        target.getTestDataNote(),
        fileCount(target),
        target.getTestDataReportedByName(),
        target.getTestDataReportedAt(),
        target.getPlan().isTestDataRequested() && accessPolicy.canReport(user, target));
  }

  /** 테스트를 볼 수 있는지 먼저 확인한 뒤 대상 기관을 찾는다(볼 수 없는 테스트의 대상 여부를 드러내지 않는다). */
  private TestPlanTarget loadTarget(CurrentUser user, Long planId, Long organizationId) {
    TestPlan plan = planService.load(planId);
    accessPolicy.ensureCanView(user, plan);
    return plan.findTarget(organizationId)
        .orElseThrow(
            () -> BusinessException.notFound("TEST_TARGET_NOT_FOUND", "이 테스트의 대상 기관이 아닙니다."));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** DB 시각 칼럼과 같은 정밀도(밀리초)로 맞춘다. */
  private LocalDateTime now() {
    return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
  }
}
