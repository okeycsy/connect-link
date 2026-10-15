package com.connectlink.organization;

import com.connectlink.common.AuditedEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

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

  /** 참여 업무. 목록 화면이 여러 기관을 한꺼번에 읽으므로 묶어서 가져온다. */
  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OrganizationBusiness> participations = new ArrayList<>();

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

  /** 참여 업무. 업무 영역 순서(enum 순)로 돌려준다. */
  public Set<Business> getBusinesses() {
    if (participations.isEmpty()) {
      return Collections.emptySet();
    }
    EnumSet<Business> result = EnumSet.noneOf(Business.class);
    participations.forEach(p -> result.add(p.getBusiness()));
    return result;
  }

  public boolean participatesIn(Business business) {
    return participations.stream().anyMatch(p -> p.getBusiness() == business);
  }

  /** 이름, 이메일 도메인, 사용 여부를 고친다. 코드와 구분은 식별값이라 바꾸지 않는다. */
  public void update(String name, String emailDomain, boolean active) {
    this.name = name;
    this.emailDomain = emailDomain;
    this.active = active;
  }

  /** 참여 업무를 주어진 집합과 같게 맞춘다. 빠진 업무는 지우고 새 업무만 더한다(기존 행의 감사 정보는 유지). */
  public void replaceBusinesses(Set<Business> businesses) {
    participations.removeIf(p -> !businesses.contains(p.getBusiness()));
    for (Business business : businesses) {
      if (!participatesIn(business)) {
        participations.add(new OrganizationBusiness(this, business));
      }
    }
  }
}
