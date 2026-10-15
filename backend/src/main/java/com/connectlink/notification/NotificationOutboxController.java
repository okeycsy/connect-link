package com.connectlink.notification;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.PageResponse;
import com.connectlink.notification.dto.NotificationOutboxResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 발송함 API. 메일·매터모스트로 나갔을 내용을 관리자가 확인한다. 데모 도구 화면이 쓴다. */
@RestController
@RequestMapping("/api/v1/notification-outbox")
@Tag(name = "알림", description = "알림 센터와 발송함")
public class NotificationOutboxController {

  private final NotificationService notificationService;
  private final CurrentUserProvider currentUserProvider;

  public NotificationOutboxController(
      NotificationService notificationService, CurrentUserProvider currentUserProvider) {
    this.notificationService = notificationService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(summary = "발송함 조회", description = "최신순. 관리자만 볼 수 있다.")
  public PageResponse<NotificationOutboxResponse> list(
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return notificationService.listOutbox(
        currentUserProvider.require(), PageRequest.of(page, Math.min(size, 100)));
  }
}
