package com.connectlink.contact;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Organization;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * 정기 변경 주간에 "현행화 완료" 를 누르지 않은 기관 하나에 보낸 리마인드 메일 한 번. 결제원 담당자가 화면에서 보내거나({@code manual}) 예약 작업이 간격마다
 * 보낸다.
 */
@Entity
@Table(name = "update_campaign_reminder")
public class UpdateCampaignReminder extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private UpdateCampaign campaign;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt;

  @Column(name = "manual", nullable = false)
  private boolean manual;

  /** 화면에서 보낸 결제원 담당자 이름. 예약 작업이 보냈으면 null. */
  @Column(name = "sender_name", length = 100)
  private String senderName;

  @Column(name = "recipient_count", nullable = false)
  private int recipientCount;

  protected UpdateCampaignReminder() {}

  UpdateCampaignReminder(
      UpdateCampaign campaign,
      Organization organization,
      LocalDateTime sentAt,
      String senderName,
      int recipientCount) {
    this.campaign = campaign;
    this.organization = organization;
    this.sentAt = sentAt;
    this.manual = senderName != null;
    this.senderName = senderName;
    this.recipientCount = recipientCount;
  }

  public Long getId() {
    return id;
  }

  public UpdateCampaign getCampaign() {
    return campaign;
  }

  public Organization getOrganization() {
    return organization;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public boolean isManual() {
    return manual;
  }

  public String getSenderName() {
    return senderName;
  }

  public int getRecipientCount() {
    return recipientCount;
  }
}
