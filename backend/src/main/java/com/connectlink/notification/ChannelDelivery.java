package com.connectlink.notification;

/** 발송함 한 건을 한 채널로 실제로 내보낸다. 채널마다 빈 하나({@link MailDelivery}, {@link MattermostDelivery}). */
public interface ChannelDelivery {

  NotificationChannel channel();

  /** 설정이 갖춰져 보낼 수 있는지. 아니면 그 채널 건은 발송 대기로 남는다. */
  boolean available();

  /**
   * 보낸다. 실패하면 예외를 던진다(발송기가 횟수와 까닭을 남긴다).
   *
   * @throws Exception 보내지 못했을 때
   */
  void deliver(NotificationOutbox outbox) throws Exception;
}
