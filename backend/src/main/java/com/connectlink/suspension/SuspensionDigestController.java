package com.connectlink.suspension;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.suspension.dto.SuspensionDigestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
