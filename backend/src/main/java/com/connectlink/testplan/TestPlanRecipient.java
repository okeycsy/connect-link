package com.connectlink.testplan;

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

/** 기관이 결과를 등록하면 매터모스트 알림을 받을 결제원 담당자 한 명. 고를 때의 이메일·이름을 남긴다. */
@Entity
@Table(name = "test_plan_recipient")
public class TestPlanRecipient extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_plan_id")
  private TestPlan plan;

  /** 계정 ID. 탈퇴하면 알림 대상에서 뺄 때 쓴다. */
  @Column(name = "account_id")
  private Long accountId;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  protected TestPlanRecipient() {}

  /** 고른 결제원 담당자 한 명의 값. */
  public record Draft(Long accountId, String email, String name) {}

  TestPlanRecipient(TestPlan plan, Long accountId, String email, String name) {
    this.plan = plan;
    this.accountId = accountId;
    this.email = email;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public TestPlan getPlan() {
    return plan;
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
}
