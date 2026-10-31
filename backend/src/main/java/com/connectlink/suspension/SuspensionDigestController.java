package com.connectlink.suspension;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.suspension.dto.SuspensionDigestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 전 영업일 취합. 정해진 때는 예약 작업이 보내고, 결제원 담당자는 이 API 로 바로 보낸다. */
@RestController
@RequestMapping("/api/v1/suspension-digests")
@Tag(name = "일시중단", description = "참가기관 일시중단 등록과 검토·확정")
public class SuspensionDigestController {

  private final SuspensionDigestService digestService;
  private final CurrentUserProvider currentUserProvider;

  public SuspensionDigestController(
      SuspensionDigestService digestService, CurrentUserProvider currentUserProvider) {
    this.digestService = digestService;
    this.currentUserProvider = currentUserProvider;
  }

  @PostMapping
  @Operation(
      summary = "전 영업일 취합 수동으로 보내기",
      description =
          "오늘을 보내는 영업일로 보고 다음 영업일까지 시작하는 확정 일시중단을 운영·관제 매터모스트 채널로 바로 보낸다. 결제원 담당자와 관리자만 부른다.")
  public SuspensionDigestResponse send() {
    return digestService.sendNow(currentUserProvider.require());
  }

  @PostMapping("/weekly")
  @Operation(
      summary = "주간 취합 수동으로 보내기",
      description =
          "이번 주(월요일부터 지금까지) 확정된 일시중단 중 이 업무 관련 건의 기관 이름, 중단 기간, 조치사항을 운영·관제 매터모스트 채널로 바로 보낸다. 자동 발송은 그 주 마지막 영업일과 그 두 영업일 앞(보통 수·금) 15시에 업무마다 따로 간다. 그 업무 영역의 결제원 담당자와 관리자만 부른다.")
  public SuspensionDigestResponse sendWeekly(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return digestService.sendWeeklyNow(
        currentUserProvider.require(), SuspensionController.resolveBusiness(business));
  }
}
