package com.connectlink.notification.demo;

import com.connectlink.notification.NotificationService;
import com.connectlink.notification.dto.NotificationOutboxResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데모 도구: 받은 메일함. 프로토타입은 메일을 실제로 보내지 않고 발송함에 남기므로, 가입 화면이 로그인 전에도 인증 코드 메일을 보여 줄 수 있게 받는 사람별 최근 메일을
 * 연다. local·demo·test 프로필에만 있다.
 */
@RestController
@RequestMapping("/api/v1/demo/mailbox")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class DemoMailboxController {

  private final NotificationService notificationService;

  public DemoMailboxController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  @Operation(summary = "받은 메일함 (받는 사람별 최근 메일 5건, 로그인 전에도 열림)")
  public List<NotificationOutboxResponse> list(
      @Parameter(description = "받는 사람 이메일. 대소문자를 가리지 않는다") @RequestParam String email) {
    return notificationService.listRecentEmailsTo(email.trim().toLowerCase(Locale.ROOT));
  }
}
