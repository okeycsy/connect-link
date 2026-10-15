package com.connectlink.notification;

import org.springframework.stereotype.Component;

/** 프로토타입 발송기. 실제로 보내는 대신 발송함에 {@code PENDING} 으로 남긴다. 화면의 발송함이 이 기록을 보여 준다. */
@Component
public class OutboxNotificationSender implements NotificationSender {

  private final NotificationOutboxRepository outboxRepository;

  public OutboxNotificationSender(NotificationOutboxRepository outboxRepository) {
    this.outboxRepository = outboxRepository;
  }

  @Override
  public void send(OutboundMessage message) {
    outboxRepository.save(
        new NotificationOutbox(
            message.notificationId(),
            message.channel(),
            message.recipient(),
            message.subject(),
            message.body()));
  }
}
