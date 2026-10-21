package com.connectlink.chat;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

/**
 * 채팅방 하나. 업무 단체방({@link ChatRoomType#BUSINESS})의 구성원은 그 업무를 맡은 사용 중 계정이라 저장하지 않고, 따로 초대한 사람만 {@link
 * ChatRoomMember} 로 둔다. 초대방({@link ChatRoomType#CUSTOM})은 초대한 사람(만든 사람 포함)이 구성원이다.
 */
@Entity
@Table(name = "chat_room")
public class ChatRoom extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private ChatRoomType type;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "created_by_name", nullable = false, length = 50)
  private String createdByName;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<ChatRoomMember> members = new ArrayList<>();

  protected ChatRoom() {}

  ChatRoom(Business business, ChatRoomType type, String name, String createdByName) {
    this.business = business;
    this.type = type;
    this.name = name;
    this.createdByName = createdByName;
  }

  /** 업무 단체방. 이름은 "{업무} 단체방" 이다. */
  static ChatRoom businessRoom(Business business) {
    return new ChatRoom(business, ChatRoomType.BUSINESS, business.getLabel() + " 단체방", "연결고리");
  }

  /** 초대한 사람을 더한다. 이미 있으면 그대로 둔다. */
  void invite(ChatRoomMember.Snapshot person, String invitedByName) {
    if (findMember(person.email()).isEmpty()) {
      members.add(new ChatRoomMember(this, person, invitedByName));
    }
  }

  /** 초대한 사람을 뺀다. 뺐으면 true. */
  boolean removeMember(String email) {
    return members.removeIf(m -> m.getEmail().equals(email));
  }

  Optional<ChatRoomMember> findMember(String email) {
    return members.stream().filter(m -> m.getEmail().equals(email)).findFirst();
  }

  public Long getId() {
    return id;
  }

  public Business getBusiness() {
    return business;
  }

  public ChatRoomType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String getCreatedByName() {
    return createdByName;
  }

  public List<ChatRoomMember> getMembers() {
    return members;
  }
}
