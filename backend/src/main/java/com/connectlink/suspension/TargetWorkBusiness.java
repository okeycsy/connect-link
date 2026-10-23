package com.connectlink.suspension;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
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

/** 대상 업무의 관련 레일 업무 하나. */
@Entity
@Table(name = "target_work_business")
public class TargetWorkBusiness extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_work_id")
  private TargetWork targetWork;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  protected TargetWorkBusiness() {}

  TargetWorkBusiness(TargetWork targetWork, Business business) {
    this.targetWork = targetWork;
    this.business = business;
  }

  public Long getId() {
    return id;
  }

  public Business getBusiness() {
    return business;
  }
}
