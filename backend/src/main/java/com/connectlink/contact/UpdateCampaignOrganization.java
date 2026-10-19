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
 * 정기 변경 주간의 기관 하나. 기관이 "현행화 완료" 를 누르면 완료, 주간 동안 그 기관 담당자 정보가 등록·수정·삭제되면 정보 변경이다. 아무 일도 없던 기관은 행이 없고
 * 미완료로 본다.
 */
@Entity
@Table(name = "update_campaign_organization")
public class UpdateCampaignOrganization extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "campaign_id", nullable = false)
  private UpdateCampaign campaign;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "completed_by_name", length = 100)
  private String completedByName;

  @Column(name = "changed_at")
  private LocalDateTime changedAt;

  protected UpdateCampaignOrganization() {}

  UpdateCampaignOrganization(UpdateCampaign campaign, Organization organization) {
    this.campaign = campaign;
    this.organization = organization;
  }

  /** 현행화 완료. 다시 누르면 완료 일시와 사람을 새로 남긴다. */
  void complete(String byName, LocalDateTime now) {
    this.completedAt = now;
    this.completedByName = byName;
  }

  /** 주간 동안 담당자 정보가 바뀌었다. 마지막으로 바뀐 때를 남긴다. */
  void markChanged(LocalDateTime now) {
    this.changedAt = now;
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

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public String getCompletedByName() {
    return completedByName;
  }

  public LocalDateTime getChangedAt() {
    return changedAt;
  }

  public boolean isCompleted() {
    return completedAt != null;
  }

  public boolean isChanged() {
    return changedAt != null;
  }
}
