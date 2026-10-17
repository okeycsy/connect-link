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

/**
 * 대상 기관의 회신 담당자 한 명. 결제원 담당자가 그 기관의 같은 업무 담당자(계정) 중에서 세부업무를 보고 고른다. 의견 요청·리마인드 메일을 받는다. 고를 때의
 * 이메일·이름·세부업무를 남기고, 계정이 탈퇴하면 이 행을 지운다.
 */
@Entity
@Table(name = "opinion_target_recipient")
public class OpinionTargetRecipient extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_id", nullable = false)
  private OpinionTarget target;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "sub_business", length = 200)
  private String subBusiness;

  protected OpinionTargetRecipient() {}

  OpinionTargetRecipient(
      OpinionTarget target, Long accountId, OpinionTarget.RecipientSnapshot snapshot) {
    this.target = target;
    this.accountId = accountId;
    this.email = snapshot.email();
    this.name = snapshot.name();
    this.subBusiness = snapshot.subBusiness();
  }

  public Long getId() {
    return id;
  }

  public OpinionTarget getTarget() {
    return target;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getSubBusiness() {
    return subBusiness;
  }
}
