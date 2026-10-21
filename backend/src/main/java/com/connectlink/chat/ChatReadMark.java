package com.connectlink.chat;

import com.connectlink.common.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 한 사람이 한 방에서 어디까지 읽었는지. 읽지 않은 사람 수와 읽지 않은 메시지 수를 여기서 센다. 앞으로만 간다. */
@Entity
@Table(name = "chat_read_mark")
public class ChatReadMark extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "last_read_message_id", nullable = false)
  private Long lastReadMessageId;

  protected ChatReadMark() {}

  ChatReadMark(Long roomId, String email, Long lastReadMessageId) {
    this.roomId = roomId;
    this.email = email;
    this.lastReadMessageId = lastReadMessageId;
  }

  /** 더 뒤의 메시지까지 읽었으면 옮긴다. */
  void advanceTo(Long messageId) {
    if (messageId > lastReadMessageId) {
      this.lastReadMessageId = messageId;
    }
  }

  public Long getId() {
    return id;
  }

  public Long getRoomId() {
    return roomId;
  }

  public String getEmail() {
    return email;
  }

  public Long getLastReadMessageId() {
    return lastReadMessageId;
  }
}
