package com.connectlink.contact;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 정기 변경(현행화) 주간. 결제원 담당자가 업무마다 기간을 등록하고, 그 기간에 기관 담당자가 자기 기관 담당자 정보를 확인해 "현행화 완료" 를 누른다. 기관별 진행은
 * {@link UpdateCampaignOrganization} 에 있다.
 */
@Entity
@Table(name = "update_campaign")
public class UpdateCampaign extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "registrant_name", nullable = false, length = 100)
  private String registrantName;

  protected UpdateCampaign() {}

  public UpdateCampaign(
      Business business,
      String title,
      LocalDate startDate,
      LocalDate endDate,
      String registrantName) {
    this.business = business;
    this.title = title;
    this.startDate = startDate;
    this.endDate = endDate;
    this.registrantName = registrantName;
  }

  public void change(String title, LocalDate startDate, LocalDate endDate) {
    this.title = title;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public UpdateCampaignStatus statusOn(LocalDate today) {
    return UpdateCampaignStatus.of(startDate, endDate, today);
  }

  public Long getId() {
    return id;
  }

  public Business getBusiness() {
    return business;
  }

  public String getTitle() {
    return title;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getRegistrantName() {
    return registrantName;
  }
}
