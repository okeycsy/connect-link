package com.connectlink.account;

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

/** 계정의 담당 업무 한 건과 그 업무의 세부업무(자유 텍스트). */
@Entity
@Table(name = "user_account_business")
public class UserAccountBusiness extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private UserAccount account;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  /** 세부업무. 예: 대출이동의 "신규기관 주담대". 명세상 자유 텍스트다. */
  @Column(name = "sub_business", length = 200)
  private String subBusiness;

  protected UserAccountBusiness() {}

  UserAccountBusiness(UserAccount account, Business business, String subBusiness) {
    this.account = account;
    this.business = business;
    this.subBusiness = subBusiness;
  }

  public Business getBusiness() {
    return business;
  }

  public String getSubBusiness() {
    return subBusiness;
  }

  void changeSubBusiness(String subBusiness) {
    this.subBusiness = subBusiness;
  }
}
