package com.connectlink.notification;

/**
 * 발송함 한 건을 한 채널로 실제로 내보낸다. 채널마다 빈 하나다. 메일({@link MailDelivery})과 매터모스트 팀 채널({@link
 * MattermostDelivery})이 있고, 빈이 없거나 받지 않는 건은 발송함에 기록만 남는다.
 */
public interface ChannelDelivery {

  NotificationChannel channel();

  /** 설정이 갖춰져 보낼 수 있는지. 아니면 그 채널 건은 발송 대기로 남는다. */
  boolean available();

  /** 이 건을 보낼 수 있는지. 아니면 발송 대기로 남는다(예: 매터모스트 개인 앞 메시지). 기본은 모두 보낸다. */
  default boolean accepts(NotificationOutbox outbox) {
    return true;
  }

  /**
   * 보낸다. 실패하면 예외를 던진다(발송기가 횟수와 까닭을 남긴다).
   *
   * @throws Exception 보내지 못했을 때
   */
  void deliver(NotificationOutbox outbox) throws Exception;
}
