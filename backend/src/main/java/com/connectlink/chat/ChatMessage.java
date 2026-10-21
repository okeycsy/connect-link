package com.connectlink.chat;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.OrganizationType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * 채팅 메시지 한 건. 보낸 사람의 이름·기관은 보낼 때 값으로 남긴다. 첨부(CHAT_MESSAGE)는 보낸 뒤 첨부 API 로 올리고, 보관 기간이 지나면 지우고
 * {@code attachmentsExpired} 를 켠다. 고치거나 지우지 않는다.
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private ChatRoom room;

  @Column(name = "sender_email", nullable = false, length = 200)
  private String senderEmail;

  @Column(name = "sender_name", nullable = false, length = 50)
  private String senderName;

  @Column(name = "sender_organization_code", nullable = false, length = 20)
  private String senderOrganizationCode;

  @Column(name = "sender_organization_name", nullable = false, length = 100)
  private String senderOrganizationName;

  @Enumerated(EnumType.STRING)
  @Column(name = "sender_organization_type", nullable = false, length = 30)
  private OrganizationType senderOrganizationType;

  @Column(name = "content", nullable = false, length = 2000)
  private String content;

  @Column(name = "with_attachments", nullable = false)
  private boolean withAttachments;

  @Column(name = "attachments_expired", nullable = false)
  private boolean attachmentsExpired;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  @OrderBy("id ASC")
  private List<ChatMessageMention> mentions = new ArrayList<>();

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 100)
  @OrderBy("id ASC")
  private List<ChatMessageAck> acks = new ArrayList<>();

  protected ChatMessage() {}

  ChatMessage(
      ChatRoom room,
      CurrentUser sender,
      String content,
      Collection<String> mentionOrganizationCodes,
      boolean withAttachments) {
    this.room = room;
    this.senderEmail = sender.email();
    this.senderName = sender.name();
    this.senderOrganizationCode = sender.organizationCode();
    this.senderOrganizationName = sender.organizationName();
    this.senderOrganizationType = sender.organizationType();
    this.content = content;
    this.withAttachments = withAttachments;
    mentionOrganizationCodes.forEach(code -> mentions.add(new ChatMessageMention(this, code)));
  }

  /** 확인 공감을 남긴다. 이미 남겼으면 그대로 둔다. */
  void acknowledge(CurrentUser user) {
    if (findAck(user.email()).isEmpty()) {
      acks.add(new ChatMessageAck(this, user));
    }
  }

  /** 확인 공감을 거둔다. */
  void withdrawAck(String email) {
    acks.removeIf(a -> a.getEmail().equals(email));
  }

  Optional<ChatMessageAck> findAck(String email) {
    return acks.stream().filter(a -> a.getEmail().equals(email)).findFirst();
  }

  boolean mentions(String organizationCode) {
    return mentions.stream().anyMatch(m -> m.getOrganizationCode().equals(organizationCode));
  }

  void expireAttachments() {
    this.attachmentsExpired = true;
  }

  public Long getId() {
    return id;
  }

  public ChatRoom getRoom() {
    return room;
  }

  public String getSenderEmail() {
    return senderEmail;
  }

  public String getSenderName() {
    return senderName;
  }

  public String getSenderOrganizationCode() {
    return senderOrganizationCode;
  }

  public String getSenderOrganizationName() {
    return senderOrganizationName;
  }

  public OrganizationType getSenderOrganizationType() {
    return senderOrganizationType;
  }

  public String getContent() {
    return content;
  }

  public boolean isWithAttachments() {
    return withAttachments;
  }

  public boolean isAttachmentsExpired() {
    return attachmentsExpired;
  }

  public List<ChatMessageMention> getMentions() {
    return mentions;
  }

  public List<ChatMessageAck> getAcks() {
    return acks;
  }
}
