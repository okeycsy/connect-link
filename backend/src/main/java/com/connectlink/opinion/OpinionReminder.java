package com.connectlink.opinion;

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

/** 미회신 기관에 보낸 리마인드 메일 한 번. 보낸 일시·사람은 감사 칼럼(created_at, created_by)과 {@code senderName} 이다. */
@Entity
@Table(name = "opinion_reminder")
public class OpinionReminder extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private OpinionRequest request;

  @Column(name = "sender_name", nullable = false, length = 50)
  private String senderName;

  @Column(name = "organization_count", nullable = false)
  private int organizationCount;

  @Column(name = "recipient_count", nullable = false)
  private int recipientCount;

  /** 그때 받은 기관 이름들("KB국민은행, 신한은행"). */
  @Column(name = "organization_names", nullable = false, length = 2000)
  private String organizationNames;

  protected OpinionReminder() {}

  OpinionReminder(
      OpinionRequest request,
      String senderName,
      int organizationCount,
      int recipientCount,
      String organizationNames) {
    this.request = request;
    this.senderName = senderName;
    this.organizationCount = organizationCount;
    this.recipientCount = recipientCount;
    this.organizationNames = organizationNames;
  }

  public Long getId() {
    return id;
  }

  public String getSenderName() {
    return senderName;
  }

  public int getOrganizationCount() {
    return organizationCount;
  }

  public int getRecipientCount() {
    return recipientCount;
  }

  public String getOrganizationNames() {
    return organizationNames;
  }
}
