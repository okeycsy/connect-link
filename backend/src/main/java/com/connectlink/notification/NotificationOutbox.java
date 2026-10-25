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

/** 발송함. 메일·매터모스트로 보낼 내용을 기록한다. 실제 발송은 켜 둔 서버에서만 {@link OutboxDispatcher} 가 한다. */
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

  /** 실제 발송을 해 본 횟수. */
  @Column(name = "attempts", nullable = false)
  private int attempts;

  /** 마지막 발송 실패 까닭. */
  @Column(name = "last_error", length = 500)
  private String lastError;

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

  public int getAttempts() {
    return attempts;
  }

  public String getLastError() {
    return lastError;
  }

  /** 보냈다. */
  void markSent(LocalDateTime now) {
    this.attempts++;
    this.status = OutboxStatus.SENT;
    this.sentAt = now;
    this.lastError = null;
  }

  /** 보내지 못했다. 정해진 횟수를 채우면 실패로 끝내고, 아니면 발송 대기로 두어 다음에 다시 보낸다. */
  void markAttemptFailed(String error, int maxAttempts) {
    this.attempts++;
    this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    if (attempts >= maxAttempts) {
      this.status = OutboxStatus.FAILED;
    }
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }
}
