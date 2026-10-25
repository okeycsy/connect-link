package com.connectlink.testplan.demo;

import com.connectlink.testplan.TestPlanService;
import com.connectlink.testplan.dto.TestPlanDueReminderRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시연용: 기관 테스트 기한 하루 전 자동 리마인드 예약 작업을 지금 한 번 돌린다(CLAUDE.md 6절 "예약 작업"). local·demo·test 프로필에만 있다.
 */
@RestController
@RequestMapping("/api/v1/demo/test-plan-due-reminder-runs")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class TestPlanDemoController {

  private final TestPlanService testPlanService;

  public TestPlanDemoController(TestPlanService testPlanService) {
    this.testPlanService = testPlanService;
  }

  @PostMapping
  @Operation(
      summary = "기관 테스트 기한 하루 전 리마인드 지금 보내기(데모)",
      description =
          "매일 9시 예약 작업과 같다. 수행 종료일이 내일이면 결과를 다 등록하지 않은 기관에, 테스트데이터 회신 기한이 내일이면 생성 완료하지 않은 기관에 보낸다. 받은"
              + " 기관 수를 돌려준다.")
  public TestPlanDueReminderRunResponse run() {
    return new TestPlanDueReminderRunResponse(testPlanService.remindDueTomorrow());
  }
}
