package com.connectlink.notification;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 발송함. 메일·매터모스트로 보낼 내용을 기록한다. 프로토타입은 여기까지만 하고 실제로 보내지 않는다. */
@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 같이 만들어진 알림 센터 알림. 알림 없이 보내는 메시지(취합 발송 등)는 비어 있다. */
  @Column(name = "notification_id")
  private Long notificationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 20)
  private NotificationChannel channel;

  /** 메일 주소 또는 매터모스트 대상(@handle, 채널). */
  @Column(name = "recipient", nullable = false, length = 300)
  private String recipient;

  @Column(name = "subject", length = 200)
  private String subject;

  @Column(name = "body", length = 2000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private OutboxStatus status = OutboxStatus.PENDING;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  protected NotificationOutbox() {}

  public NotificationOutbox(
      Long notificationId,
      NotificationChannel channel,
      String recipient,
      String subject,
      String body) {
    this.notificationId = notificationId;
    this.channel = channel;
    this.recipient = recipient;
    this.subject = subject;
    this.body = body;
  }

  public Long getId() {
    return id;
  }

  public Long getNotificationId() {
    return notificationId;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }
}
