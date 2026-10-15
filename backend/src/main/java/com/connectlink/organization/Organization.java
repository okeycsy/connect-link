package com.connectlink.organization;

import com.connectlink.common.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 기관. 결제원, 참가기관, 이용기관을 모두 담는 상위 개념이다. */
@Entity
@Table(name = "organization")
public class Organization extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 기관 코드. 화면 표시용이 아니라 식별용이며 바뀌지 않는다. */
  @Column(name = "code", nullable = false, unique = true, length = 20)
  private String code;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private OrganizationType type;

  /** 회사 이메일 도메인. 가입 승인 화면에서 신청자 이메일과 기관이 맞는지 볼 때 쓴다. */
  @Column(name = "email_domain", length = 100)
  private String emailDomain;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected Organization() {}

  public Organization(String code, String name, OrganizationType type, String emailDomain) {
    this.code = code;
    this.name = name;
    this.type = type;
    this.emailDomain = emailDomain;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public OrganizationType getType() {
    return type;
  }

  public String getEmailDomain() {
    return emailDomain;
  }

  public boolean isActive() {
    return active;
  }
}
