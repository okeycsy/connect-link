package com.connectlink.notification;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 알림 센터의 알림 한 건. 받는 사람은 회사 이메일로 식별한다(계정 모듈이 붙어도 같은 키를 쓴다). */
@Entity
@Table(name = "notification")
public class Notification extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "recipient_email", nullable = false, length = 200)
  private String recipientEmail;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 40)
  private NotificationType type;

  /** 어느 업무 영역의 알림인지. 가입 승인처럼 업무와 무관하면 비어 있다. */
  @Enumerated(EnumType.STRING)
  @Column(name = "business", length = 30)
  private Business business;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "body", length = 1000)
  private String body;

  /** 눌렀을 때 열 화면 주소(예: /account-info/opinions/12). */
  @Column(name = "link", length = 300)
  private String link;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  protected Notification() {}

  public Notification(
      String recipientEmail,
      NotificationType type,
      Business business,
      String title,
      String body,
      String link) {
    this.recipientEmail = recipientEmail;
    this.type = type;
    this.business = business;
    this.title = title;
    this.body = body;
    this.link = link;
  }

  public void markRead(LocalDateTime now) {
    if (readAt == null) {
      readAt = now;
    }
  }

  public boolean isRead() {
    return readAt != null;
  }

  public Long getId() {
    return id;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public NotificationType getType() {
    return type;
  }

  public Business getBusiness() {
    return business;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getLink() {
    return link;
  }

  public LocalDateTime getReadAt() {
    return readAt;
  }
}
