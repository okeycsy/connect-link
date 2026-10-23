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

/** 일시중단이 멈추는 업무 하나. 한 건이 여러 업무를 멈출 수 있다(대외계·네트워크 작업). */
@Entity
@Table(name = "suspension_business")
public class SuspensionBusiness extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "suspension_id")
  private Suspension suspension;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  protected SuspensionBusiness() {}

  SuspensionBusiness(Suspension suspension, Business business) {
    this.suspension = suspension;
    this.business = business;
  }

  public Long getId() {
    return id;
  }

  public Business getBusiness() {
    return business;
  }
}
