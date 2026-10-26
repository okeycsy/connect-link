package com.connectlink.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 메일 발송. 메일 서버는 Spring Boot 설정({@code spring.mail.host} 등)으로 정하고, 설정이 없으면 {@link JavaMailSender} 빈이
 * 없어 보내지 않는다. 본문은 일반 텍스트다.
 */
@Component
class MailDelivery implements ChannelDelivery {

  private final ObjectProvider<JavaMailSender> mailSender;
  private final NotificationDeliveryProperties properties;

  MailDelivery(
      ObjectProvider<JavaMailSender> mailSender, NotificationDeliveryProperties properties) {
    this.mailSender = mailSender;
    this.properties = properties;
  }

  @Override
  public NotificationChannel channel() {
    return NotificationChannel.EMAIL;
  }

  @Override
  public boolean available() {
    return mailSender.getIfAvailable() != null && !properties.getMail().getFrom().isBlank();
  }

  @Override
  public void deliver(NotificationOutbox outbox) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(properties.getMail().getFrom());
    message.setTo(outbox.getRecipient());
    message.setSubject(
        outbox.getSubject() == null ? MailSubject.service("알림") : outbox.getSubject());
    message.setText(outbox.getBody() == null ? "" : outbox.getBody());
    mailSender.getObject().send(message);
  }
}
