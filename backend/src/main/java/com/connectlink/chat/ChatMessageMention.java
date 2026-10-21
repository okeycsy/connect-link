package com.connectlink.chat;

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

/** 메시지의 기관 멘션 하나(예: "@결제원", "@KB국민은행"). 기관 코드로 남기고 결제원은 KFTC 다. */
@Entity
@Table(name = "chat_message_mention")
public class ChatMessageMention extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "message_id", nullable = false)
  private ChatMessage message;

  @Column(name = "organization_code", nullable = false, length = 20)
  private String organizationCode;

  protected ChatMessageMention() {}

  ChatMessageMention(ChatMessage message, String organizationCode) {
    this.message = message;
    this.organizationCode = organizationCode;
  }

  public Long getId() {
    return id;
  }

  public String getOrganizationCode() {
    return organizationCode;
  }
}
