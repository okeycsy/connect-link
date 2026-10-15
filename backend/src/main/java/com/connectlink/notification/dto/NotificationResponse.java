package com.connectlink.notification.dto;

import com.connectlink.notification.Notification;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 알림 한 건. */
public record NotificationResponse(
    Long id,
    NotificationType type,
    String typeLabel,
    @Schema(nullable = true) Business business,
    @Schema(nullable = true) String businessSlug,
    @Schema(nullable = true) String businessLabel,
    String title,
    @Schema(nullable = true) String body,
    @Schema(nullable = true) String link,
    boolean read,
    @Schema(nullable = true) LocalDateTime readAt,
    LocalDateTime createdAt) {

  public static NotificationResponse from(Notification n) {
    Business business = n.getBusiness();
    return new NotificationResponse(
        n.getId(),
        n.getType(),
        n.getType().getLabel(),
        business,
        business == null ? null : business.getSlug(),
        business == null ? null : business.getLabel(),
        n.getTitle(),
        n.getBody(),
        n.getLink(),
        n.isRead(),
        n.getReadAt(),
        n.getCreatedAt());
  }
}
