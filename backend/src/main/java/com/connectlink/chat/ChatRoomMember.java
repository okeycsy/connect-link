package com.connectlink.chat;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.OrganizationType;
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
import javax.persistence.Table;

/** 방에 초대한 사람 한 명. 초대할 때의 이름·기관을 남긴다. 계정이 탈퇴하면 지운다. */
@Entity
@Table(name = "chat_room_member")
public class ChatRoomMember extends AuditedEntity {

  /** 초대할 때의 사람 정보. */
  record Snapshot(
      String email,
      String name,
      String organizationCode,
      String organizationName,
      OrganizationType organizationType) {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private ChatRoom room;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "organization_code", nullable = false, length = 20)
  private String organizationCode;

  @Column(name = "organization_name", nullable = false, length = 100)
  private String organizationName;

  @Enumerated(EnumType.STRING)
  @Column(name = "organization_type", nullable = false, length = 30)
  private OrganizationType organizationType;

  @Column(name = "invited_by_name", nullable = false, length = 50)
  private String invitedByName;

  protected ChatRoomMember() {}

  ChatRoomMember(ChatRoom room, Snapshot person, String invitedByName) {
    this.room = room;
    this.email = person.email();
    this.name = person.name();
    this.organizationCode = person.organizationCode();
    this.organizationName = person.organizationName();
    this.organizationType = person.organizationType();
    this.invitedByName = invitedByName;
  }

  Snapshot snapshot() {
    return new Snapshot(email, name, organizationCode, organizationName, organizationType);
  }

  public Long getId() {
    return id;
  }

  public ChatRoom getRoom() {
    return room;
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

  public OrganizationType getOrganizationType() {
    return organizationType;
  }

  public String getInvitedByName() {
    return invitedByName;
  }
}
