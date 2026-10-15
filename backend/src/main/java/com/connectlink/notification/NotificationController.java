package com.connectlink.notification;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.PageResponse;
import com.connectlink.notification.dto.NotificationResponse;
import com.connectlink.notification.dto.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 알림 센터 API. 로그인 사용자 본인의 알림만 다룬다. */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "알림", description = "알림 센터와 발송함")
public class NotificationController {

  private final NotificationService notificationService;
  private final CurrentUserProvider currentUserProvider;

  public NotificationController(
      NotificationService notificationService, CurrentUserProvider currentUserProvider) {
    this.notificationService = notificationService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(summary = "내 알림 목록 조회", description = "최신순. unreadOnly=true 면 읽지 않은 알림만 돌려준다.")
  public PageResponse<NotificationResponse> list(
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean unreadOnly) {
    return notificationService.list(
        currentUserProvider.require(), unreadOnly, PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/unread-count")
  @Operation(summary = "읽지 않은 알림 수 조회")
  public UnreadCountResponse unreadCount() {
    return new UnreadCountResponse(notificationService.unreadCount(currentUserProvider.require()));
  }

  @PostMapping("/{id}/read")
  @Operation(summary = "알림 읽음 처리", description = "본인 알림이 아니면 403.")
  public NotificationResponse markRead(@PathVariable Long id) {
    return notificationService.markRead(currentUserProvider.require(), id);
  }

  @PostMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "모든 알림 읽음 처리")
  public void markAllRead() {
    notificationService.markAllRead(currentUserProvider.require());
  }
}
