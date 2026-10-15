package com.connectlink.notification;

/** 채널로 내보낼 메시지 한 건. {@code notificationId} 는 알림 센터 알림과 짝이 있을 때만 있다. */
public record OutboundMessage(
    NotificationChannel channel,
    String recipient,
    String subject,
    String body,
    Long notificationId) {}
