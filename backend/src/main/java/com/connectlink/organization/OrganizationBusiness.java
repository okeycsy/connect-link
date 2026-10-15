package com.connectlink.organization;

import com.connectlink.common.AuditedEntity;
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

/** 기관의 업무 참여 한 건. 레일(업무 영역)별 조회 범위의 기준이다. */
@Entity
@Table(name = "organization_business")
public class OrganizationBusiness extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  protected OrganizationBusiness() {}

  OrganizationBusiness(Organization organization, Business business) {
    this.organization = organization;
    this.business = business;
  }

  public Long getId() {
    return id;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Business getBusiness() {
    return business;
  }
}
