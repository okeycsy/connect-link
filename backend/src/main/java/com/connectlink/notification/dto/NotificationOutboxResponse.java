package com.connectlink.notification.dto;

import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationOutbox;
import com.connectlink.notification.OutboxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 발송함 한 건. */
public record NotificationOutboxResponse(
    Long id,
    @Schema(nullable = true) Long notificationId,
    NotificationChannel channel,
    String channelLabel,
    String recipient,
    @Schema(nullable = true) String subject,
    @Schema(nullable = true) String body,
    OutboxStatus status,
    String statusLabel,
    @Schema(nullable = true) LocalDateTime sentAt,
    @Schema(description = "실제 발송을 해 본 횟수") int attempts,
    @Schema(nullable = true, description = "마지막 발송 실패 까닭") String lastError,
    LocalDateTime createdAt) {

  public static NotificationOutboxResponse from(NotificationOutbox o) {
    return new NotificationOutboxResponse(
        o.getId(),
        o.getNotificationId(),
        o.getChannel(),
        o.getChannel().getLabel(),
        o.getRecipient(),
        o.getSubject(),
        o.getBody(),
        o.getStatus(),
        o.getStatus().getLabel(),
        o.getSentAt(),
        o.getAttempts(),
        o.getLastError(),
        o.getCreatedAt());
  }
}
