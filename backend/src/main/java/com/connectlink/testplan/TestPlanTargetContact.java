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

/**
 * 대상 기관의 테스트 담당자 한 명. 결제원 담당자가 그 기관의 같은 업무 담당자 연락처(계정이 없는 후임자도 있다) 중에서 세부업무를 보고 고른다. 테스트 열림·테스트데이터
 * 요청·기한 리마인드 메일을 받는다. 고를 때의 이메일·이름·세부업무를 남기고, 같은 이메일의 계정이 탈퇴하면 이 행을 지운다. 결과 등록 권한과는 상관없다(그 기관의 업무
 * 담당자 누구나 등록한다).
 */
@Entity
@Table(name = "test_plan_target_contact")
public class TestPlanTargetContact extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_plan_target_id", nullable = false)
  private TestPlanTarget target;

  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "sub_business", length = 200)
  private String subBusiness;

  protected TestPlanTargetContact() {}

  TestPlanTargetContact(TestPlanTarget target, Snapshot snapshot) {
    this.target = target;
    this.email = snapshot.email();
    this.name = snapshot.name();
    this.subBusiness = snapshot.subBusiness();
  }

  public Long getId() {
    return id;
  }

  public TestPlanTarget getTarget() {
    return target;
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

  /** 고를 때의 담당자 연락처 값. */
  record Snapshot(String email, String name, String subBusiness) {}
}
