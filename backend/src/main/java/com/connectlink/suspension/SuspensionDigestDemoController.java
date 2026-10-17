package com.connectlink.suspension;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.suspension.dto.SuspensionDigestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 데모 도구: 전 영업일 취합 전달을 예약 시각을 기다리지 않고 바로 보낸다. 보낸 글은 발송함(매터모스트)에 남는다. local·demo·test 프로필에만 있다. */
@RestController
@RequestMapping("/api/v1/demo/suspension-digest")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class SuspensionDigestDemoController {

  private final SuspensionDigestService digestService;
  private final CurrentUserProvider currentUserProvider;

  public SuspensionDigestDemoController(
      SuspensionDigestService digestService, CurrentUserProvider currentUserProvider) {
    this.digestService = digestService;
    this.currentUserProvider = currentUserProvider;
  }

  @PostMapping
  @Operation(
      summary = "일시중단 전 영업일 취합 지금 보내기",
      description = "오늘을 보내는 영업일로 보고 다음 영업일까지 시작하는 확정 건을 운영·관제 채널로 보낸다. 결제원 담당자만 부른다.")
  public SuspensionDigestResponse sendNow() {
    return digestService.sendNow(currentUserProvider.require());
  }
}
