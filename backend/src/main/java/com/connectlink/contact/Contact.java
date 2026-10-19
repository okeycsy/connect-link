package com.connectlink.contact;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * 담당자 연락처. 기관 하나의 업무 하나를 맡은 사람 한 명이다. 계정이 없는 담당자(후임자 등)도 있으므로 계정을 참조하지 않고, 본인 여부는 회사 이메일로 맞춘다. 고치거나
 * "변동 없음" 으로 확인하면 최근 확인일({@code lastConfirmedAt})이 갱신된다.
 */
@Entity
@Table(name = "contact")
public class Contact extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  @Column(name = "sub_business", length = 200)
  private String subBusiness;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "department", nullable = false, length = 100)
  private String department;

  @Column(name = "team", length = 100)
  private String team;

  @Column(name = "job_title", length = 50)
  private String jobTitle;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_role", nullable = false, length = 20)
  private JobRole jobRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_rank", nullable = false, length = 20)
  private ContactRank rank;

  /** 내선(사무실) 번호. 이전 담당자 이력에서도 그대로 보이는 번호다. */
  @Column(name = "extension_phone", nullable = false, length = 30)
  private String extensionPhone;

  /** 휴대폰. 이전 담당자 이력에서는 마스킹된다. */
  @Column(name = "mobile_phone", length = 30)
  private String mobilePhone;

  /** 회사 이메일(소문자). 로그인한 사람의 본인 행을 찾는 키다. */
  @Column(name = "email", nullable = false, length = 200)
  private String email;

  @Column(name = "last_confirmed_at", nullable = false)
  private LocalDateTime lastConfirmedAt;

  @Column(name = "last_confirmed_by_name", nullable = false, length = 100)
  private String lastConfirmedByName;

  /** 6개월 미확인 알림을 보낸 때. 확인일보다 뒤면 이번 미확인 기간에는 이미 보냈다. */
  @Column(name = "reconfirm_notified_at")
  private LocalDateTime reconfirmNotifiedAt;

  @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<ContactItRole> itRoles = new ArrayList<>();

  protected Contact() {}

  /** 양식 값. 글은 앞뒤 공백을 뗀 값이고 이메일은 소문자다. */
  public record Content(
      String subBusiness,
      String name,
      String department,
      String team,
      String jobTitle,
      JobRole jobRole,
      Set<ItRole> itRoles,
      ContactRank rank,
      String extensionPhone,
      String mobilePhone,
      String email) {}

  public static Contact register(
      Organization organization,
      Business business,
      Content content,
      String confirmedByName,
      LocalDateTime now) {
    Contact contact = new Contact();
    contact.organization = organization;
    contact.business = business;
    contact.change(content);
    contact.confirm(confirmedByName, now);
    return contact;
  }

  /** 내용을 바꾼다. 최근 확인일은 부르는 쪽이 {@link #confirm} 으로 따로 갱신한다. */
  public void change(Content content) {
    this.subBusiness = content.subBusiness();
    this.name = content.name();
    this.department = content.department();
    this.team = content.team();
    this.jobTitle = content.jobTitle();
    this.jobRole = content.jobRole();
    this.rank = content.rank();
    this.extensionPhone = content.extensionPhone();
    this.mobilePhone = content.mobilePhone();
    this.email = content.email();
    replaceItRoles(content.jobRole() == JobRole.IT ? content.itRoles() : Set.of());
  }

  /** 최근 확인일을 지금으로 한다. 고쳤을 때와 "변동 없음" 확인 때 부른다. */
  public void confirm(String byName, LocalDateTime now) {
    this.lastConfirmedAt = now;
    this.lastConfirmedByName = byName;
  }

  /** 6개월 미확인 알림을 보냈다고 남긴다. */
  public void markReconfirmNotified(LocalDateTime now) {
    this.reconfirmNotifiedAt = now;
  }

  /** 이번 미확인 기간에 아직 알림을 보내지 않았는지. 확인한 뒤에 보낸 알림이 있으면 보냈다. */
  public boolean isReconfirmNotificationPending() {
    return reconfirmNotifiedAt == null || reconfirmNotifiedAt.isBefore(lastConfirmedAt);
  }

  private void replaceItRoles(Set<ItRole> requested) {
    Set<ItRole> wanted = requested == null ? Set.of() : requested;
    itRoles.removeIf(r -> !wanted.contains(r.getItRole()));
    Set<ItRole> current = getItRoles();
    wanted.stream()
        .filter(role -> !current.contains(role))
        .sorted()
        .forEach(role -> itRoles.add(new ContactItRole(this, role)));
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

  public String getSubBusiness() {
    return subBusiness;
  }

  public String getName() {
    return name;
  }

  public String getDepartment() {
    return department;
  }

  public String getTeam() {
    return team;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public JobRole getJobRole() {
    return jobRole;
  }

  public ContactRank getRank() {
    return rank;
  }

  public String getExtensionPhone() {
    return extensionPhone;
  }

  public String getMobilePhone() {
    return mobilePhone;
  }

  public String getEmail() {
    return email;
  }

  public LocalDateTime getLastConfirmedAt() {
    return lastConfirmedAt;
  }

  public String getLastConfirmedByName() {
    return lastConfirmedByName;
  }

  public LocalDateTime getReconfirmNotifiedAt() {
    return reconfirmNotifiedAt;
  }

  /** 전산 세부 직무. 선언 순서다. */
  public Set<ItRole> getItRoles() {
    EnumSet<ItRole> result = EnumSet.noneOf(ItRole.class);
    itRoles.forEach(r -> result.add(r.getItRole()));
    return result;
  }

  /** 직무 표기(예: "전산(운영, 상황실)"). */
  public String jobRoleLabel() {
    return jobRole.describe(getItRoles());
  }
}
