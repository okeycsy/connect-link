package com.connectlink.chat;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** 메시지에 남긴 "확인" 공감 하나. 남긴 사람의 이름·기관을 그때 값으로 둔다. 메시지 아래에 "KB국민은행 확인" 꼴로 보인다. */
@Entity
@Table(name = "chat_message_ack")
public class ChatMessageAck extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "message_id", nullable = false)
  private ChatMessage message;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "organization_code", nullable = false, length = 20)
  private String organizationCode;

  @Column(name = "organization_name", nullable = false, length = 100)
  private String organizationName;

  protected ChatMessageAck() {}

  ChatMessageAck(ChatMessage message, CurrentUser user) {
    this.message = message;
    this.email = user.email();
    this.name = user.name();
    this.organizationCode = user.organizationCode();
    this.organizationName = user.organizationName();
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getOrganizationCode() {
    return organizationCode;
  }

  public String getOrganizationName() {
    return organizationName;
  }
}
