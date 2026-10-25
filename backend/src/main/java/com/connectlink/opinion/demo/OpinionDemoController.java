package com.connectlink.opinion.demo;

import com.connectlink.opinion.OpinionCollationService;
import com.connectlink.opinion.dto.DueReminderRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 시연용: 의견 회신 기한 하루 전 자동 리마인드 예약 작업을 지금 한 번 돌린다(CLAUDE.md 6절 "예약 작업"). local·demo·test 프로필에만 있다. */
@RestController
@RequestMapping("/api/v1/demo/opinion-due-reminder-runs")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class OpinionDemoController {

  private final OpinionCollationService collationService;

  public OpinionDemoController(OpinionCollationService collationService) {
    this.collationService = collationService;
  }

  @PostMapping
  @Operation(
      summary = "의견 회신 기한 하루 전 리마인드 지금 보내기(데모)",
      description = "매일 9시 예약 작업과 같다. 회신 기한이 내일인 회신중 요청의 미회신 기관에만 보내고, 같은 기한으로 이미 보낸 요청은 건너뛴다.")
  public DueReminderRunResponse run() {
    return new DueReminderRunResponse(collationService.remindDueTomorrow());
  }
}
