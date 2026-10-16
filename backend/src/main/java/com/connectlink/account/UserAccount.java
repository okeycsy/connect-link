package com.connectlink.account;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * 계정. 로그인 주체이며 가입 때 받은 소속·업무·직무·연락처 정보를 함께 가진다. 연락처 정보는 승인 뒤 연락처 모듈이 넘겨받는다(docs/decisions.md). 식별
 * 키는 소문자로 맞춘 회사 이메일이다.
 */
@Entity
@Table(name = "user_account")
public class UserAccount extends AuditedEntity {

  /** 탈퇴한 계정의 이름. */
  public static final String WITHDRAWN_NAME = "탈퇴한 사용자";

  /** 탈퇴한 계정의 비밀번호 칸. BCrypt 형식이 아니라 어떤 비밀번호와도 맞지 않는다. */
  private static final String WITHDRAWN_PASSWORD_HASH = "!withdrawn";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, unique = true, length = 200)
  private String email;

  /** BCrypt 해시. 원문은 어디에도 남기지 않는다. */
  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private AccountStatus status;

  /** 포털 관리자. 기준정보 관리 같은 관리 기능을 쓴다. 가입으로는 생기지 않는다. */
  @Column(name = "admin", nullable = false)
  private boolean admin;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_role", nullable = false, length = 20)
  private JobRole jobRole;

  @Column(name = "department", nullable = false, length = 100)
  private String department;

  @Column(name = "team", nullable = false, length = 100)
  private String team;

  /** 직급. */
  @Column(name = "job_title", nullable = false, length = 50)
  private String jobTitle;

  /** 내선번호(필수). */
  @Column(name = "extension_phone", nullable = false, length = 30)
  private String extensionPhone;

  /** 휴대폰번호(선택). */
  @Column(name = "mobile_phone", length = 30)
  private String mobilePhone;

  /** 가입 신청 일시. 반려 뒤 다시 신청하면 그때로 바뀐다. */
  @Column(name = "applied_at", nullable = false)
  private LocalDateTime appliedAt;

  /** 승인·반려한 일시. 승인 대기면 없다. */
  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  /** 승인·반려한 결제원 담당자의 이메일과 그때 이름. */
  @Column(name = "reviewer_email", length = 200)
  private String reviewerEmail;

  @Column(name = "reviewer_name", length = 50)
  private String reviewerName;

  /** 반려 사유. 반려 메일에 그대로 들어간다. */
  @Column(name = "rejection_reason", length = 500)
  private String rejectionReason;

  /** 탈퇴 일시. 탈퇴하지 않았으면 없다. */
  @Column(name = "withdrawn_at")
  private LocalDateTime withdrawnAt;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<UserAccountBusiness> businesses = new ArrayList<>();

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<UserAccountItRole> itRoles = new ArrayList<>();

  protected UserAccount() {}

  /**
   * 가입 신청. 승인 대기 상태로 만들고 관리자 권한은 주지 않는다. 담당 업무는 업무와 세부업무 짝이며, 전산 세부 직무는 직무가 전산일 때만 남긴다.
   *
   * @param businesses 업무 → 세부업무(없으면 null). 넣은 순서와 상관없이 업무 순서로 보인다
   */
  public static UserAccount apply(
      String email,
      String passwordHash,
      String name,
      Organization organization,
      JobRole jobRole,
      Set<ItRole> itRoles,
      Map<Business, String> businesses,
      String department,
      String team,
      String jobTitle,
      String extensionPhone,
      String mobilePhone,
      LocalDateTime appliedAt) {
    UserAccount account = new UserAccount();
    account.email = email;
    account.admin = false;
    account.fill(
        passwordHash,
        name,
        organization,
        jobRole,
        itRoles,
        businesses,
        department,
        team,
        jobTitle,
        extensionPhone,
        mobilePhone,
        appliedAt);
    return account;
  }

  /**
   * 반려된 계정의 다시 신청. 같은 계정 행을 새 신청 내용으로 바꾸고 승인 대기로 돌린다. 지난 반려 기록(처리한 사람, 사유)은 지운다.
   *
   * @throws IllegalStateException 반려 상태가 아니면
   */
  public void reapply(
      String passwordHash,
      String name,
      Organization organization,
      JobRole jobRole,
      Set<ItRole> itRoles,
      Map<Business, String> businesses,
      String department,
      String team,
      String jobTitle,
      String extensionPhone,
      String mobilePhone,
      LocalDateTime appliedAt) {
    if (status != AccountStatus.REJECTED) {
      throw new IllegalStateException("반려된 계정만 다시 신청할 수 있습니다: " + status);
    }
    fill(
        passwordHash,
        name,
        organization,
        jobRole,
        itRoles,
        businesses,
        department,
        team,
        jobTitle,
        extensionPhone,
        mobilePhone,
        appliedAt);
  }

  private void fill(
      String passwordHash,
      String name,
      Organization organization,
      JobRole jobRole,
      Set<ItRole> itRoles,
      Map<Business, String> businesses,
      String department,
      String team,
      String jobTitle,
      String extensionPhone,
      String mobilePhone,
      LocalDateTime appliedAt) {
    this.passwordHash = passwordHash;
    this.name = name;
    this.organization = organization;
    this.status = AccountStatus.PENDING_APPROVAL;
    this.jobRole = jobRole;
    this.department = department;
    this.team = team;
    this.jobTitle = jobTitle;
    this.extensionPhone = extensionPhone;
    this.mobilePhone = mobilePhone;
    this.appliedAt = appliedAt;
    this.reviewedAt = null;
    this.reviewerEmail = null;
    this.reviewerName = null;
    this.rejectionReason = null;
    replaceBusinesses(businesses);
    replaceItRoles(jobRole == JobRole.IT ? itRoles : Set.of());
  }

  /**
   * 담당 업무를 바꾼다. 남는 업무는 행을 그대로 두고 세부업무만 고친다. 같은 업무 행을 지우고 다시 넣으면 Hibernate 가 넣기를 먼저 해서 (계정, 업무) 유일
   * 제약에 걸린다.
   */
  private void replaceBusinesses(Map<Business, String> requested) {
    businesses.removeIf(b -> !requested.containsKey(b.getBusiness()));
    requested.forEach(
        (business, subBusiness) ->
            businesses.stream()
                .filter(b -> b.getBusiness() == business)
                .findFirst()
                .ifPresentOrElse(
                    b -> b.changeSubBusiness(subBusiness),
                    () -> businesses.add(new UserAccountBusiness(this, business, subBusiness))));
  }

  private void replaceItRoles(Set<ItRole> requested) {
    itRoles.removeIf(r -> !requested.contains(r.getItRole()));
    Set<ItRole> current = getItRoles();
    requested.stream()
        .filter(role -> !current.contains(role))
        .sorted()
        .forEach(role -> itRoles.add(new UserAccountItRole(this, role)));
  }

  /**
   * 가입 승인. 사용 중 계정이 되어 로그인할 수 있다.
   *
   * @throws IllegalStateException 승인 대기가 아니면
   */
  public void approve(String reviewerEmail, String reviewerName, LocalDateTime now) {
    review(AccountStatus.ACTIVE, reviewerEmail, reviewerName, now);
    this.rejectionReason = null;
  }

  /**
   * 가입 반려. 로그인할 수 없고, 같은 이메일로 다시 신청할 수 있다.
   *
   * @throws IllegalStateException 승인 대기가 아니면
   */
  public void reject(String reviewerEmail, String reviewerName, String reason, LocalDateTime now) {
    review(AccountStatus.REJECTED, reviewerEmail, reviewerName, now);
    this.rejectionReason = reason;
  }

  private void review(
      AccountStatus next, String reviewerEmail, String reviewerName, LocalDateTime now) {
    if (status != AccountStatus.PENDING_APPROVAL) {
      throw new IllegalStateException("승인 대기 계정만 처리할 수 있습니다: " + status);
    }
    this.status = next;
    this.reviewedAt = now;
    this.reviewerEmail = reviewerEmail;
    this.reviewerName = reviewerName;
  }

  /**
   * 탈퇴. 다시 로그인할 수 없고, 이름·부서·연락처·담당 업무·전산 세부 직무·관리자 권한을 지운다. 회사 이메일은 같은 이메일로 다시 가입할 수 있게 계정마다 다른 빈
   * 주소({@code withdrawn-{id}@withdrawn.invalid})로 바꾼다. 행은 남겨 소속 기관, 가입 승인 기록, 탈퇴 일시를 둔다.
   *
   * @throws IllegalStateException 사용 중 계정이 아니면
   */
  public void withdraw(LocalDateTime now) {
    if (status != AccountStatus.ACTIVE) {
      throw new IllegalStateException("사용 중 계정만 탈퇴할 수 있습니다: " + status);
    }
    this.status = AccountStatus.WITHDRAWN;
    this.withdrawnAt = now;
    this.email = "withdrawn-" + id + "@withdrawn.invalid";
    this.passwordHash = WITHDRAWN_PASSWORD_HASH;
    this.name = WITHDRAWN_NAME;
    this.admin = false;
    this.department = "";
    this.team = "";
    this.jobTitle = "";
    this.extensionPhone = "";
    this.mobilePhone = null;
    this.businesses.clear();
    this.itRoles.clear();
  }

  /** 회사 이메일의 도메인이 소속 기관에 등록된 도메인과 같은지. 기관에 도메인이 없으면 false 다. */
  public boolean emailDomainMatches() {
    String domain = organization.getEmailDomain();
    if (domain == null || domain.isBlank()) {
      return false;
    }
    String emailDomain = email.substring(email.indexOf('@') + 1);
    return emailDomain.equals(domain.trim().toLowerCase(Locale.ROOT));
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getName() {
    return name;
  }

  public Organization getOrganization() {
    return organization;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public boolean isAdmin() {
    return admin;
  }

  public JobRole getJobRole() {
    return jobRole;
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

  public String getExtensionPhone() {
    return extensionPhone;
  }

  public String getMobilePhone() {
    return mobilePhone;
  }

  public LocalDateTime getAppliedAt() {
    return appliedAt;
  }

  public LocalDateTime getReviewedAt() {
    return reviewedAt;
  }

  public String getReviewerEmail() {
    return reviewerEmail;
  }

  public String getReviewerName() {
    return reviewerName;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public LocalDateTime getWithdrawnAt() {
    return withdrawnAt;
  }

  /** 담당 업무. 업무 영역 순서(enum 순)다. */
  public Set<Business> getBusinesses() {
    EnumSet<Business> result = EnumSet.noneOf(Business.class);
    businesses.forEach(b -> result.add(b.getBusiness()));
    return result;
  }

  /** 담당 업무와 세부업무. 업무 영역 순서다. */
  public List<UserAccountBusiness> getBusinessDetails() {
    return businesses.stream()
        .sorted(Comparator.comparing(UserAccountBusiness::getBusiness))
        .toList();
  }

  public Set<ItRole> getItRoles() {
    EnumSet<ItRole> result = EnumSet.noneOf(ItRole.class);
    itRoles.forEach(r -> result.add(r.getItRole()));
    return result;
  }
}
