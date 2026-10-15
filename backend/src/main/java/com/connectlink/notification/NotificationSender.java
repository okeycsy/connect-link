package com.connectlink.notification;

/**
 * 메일·매터모스트 발송 인터페이스. 업무 코드는 메일이나 웹훅을 직접 부르지 않고 이것만 쓴다.
 *
 * <p>프로토타입 구현은 {@link OutboxNotificationSender}(발송함 기록)다. 운영에서는 실제 발송 구현으로 바꾼다.
 */
public interface NotificationSender {

  void send(OutboundMessage message);
}
