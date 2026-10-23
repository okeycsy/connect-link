package com.connectlink.suspension;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
 * 참가기관 일시중단 한 건. 기관 담당자나 결제원 담당자가 정해진 양식({@link Content})으로 등록하고, 결제원 담당자가 확인을 시작하고({@link
 * SuspensionReviewer}) 조치·인수인계 사항을 쓰면 확정된다. 확정 건은 종료 시각이 지나면 완료다.
 */
@Entity
@Table(name = "suspension")
public class Suspension extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 중단하는 참가기관. 등록한 뒤에는 바꾸지 않는다. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  /** 중단 이유(작업 내용). */
  @Column(name = "reason", nullable = false, length = 2000)
  private String reason;

  /** 결제원 조치사항(중단 동안 결제원이 할 일). */
  @Column(name = "kftc_action", length = 2000)
  private String kftcAction;

  /** 추가 사항. */
  @Column(name = "extra_note", length = 2000)
  private String extraNote;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SuspensionStatus status;

  @Column(name = "registrant_email", nullable = false, length = 200)
  private String registrantEmail;

  @Column(name = "registrant_name", nullable = false, length = 50)
  private String registrantName;

  /** 등록한 사람의 그때 직급. 없으면 null. */
  @Column(name = "registrant_job_title", length = 50)
  private String registrantJobTitle;

  /** 결제원 담당자가 등록했는지. 아니면 기관 담당자가 올린 건이다. */
  @Column(name = "registrant_kftc", nullable = false)
  private boolean registrantKftc;

  /** 결제원 운영·개발·업무 담당자가 쓴 조치 및 인수인계 사항. 쓰면 확정된다. */
  @Column(name = "handover", length = 2000)
  private String handover;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "confirmed_by_email", length = 200)
  private String confirmedByEmail;

  @Column(name = "confirmed_by_name", length = 50)
  private String confirmedByName;

  /** 확정한 결제원 담당자의 그때 직급. 없으면 null. */
  @Column(name = "confirmed_by_job_title", length = 50)
  private String confirmedByJobTitle;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  /** 고른 대상 업무(기준정보). 하나 이상이다. */
  @OneToMany(mappedBy = "suspension", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<SuspensionTargetWork> targetWorks = new ArrayList<>();

  /**
   * 관련 레일 업무. 고른 대상 업무의 관련 업무를 저장할 때 모아 둔다(기준정보가 나중에 바뀌어도 그대로). 목록·알림·확인 범위의 기준이고, 비면 결제원
   * 운영·상황실·대외계가 맡는다.
   */
  @OneToMany(mappedBy = "suspension", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<SuspensionBusiness> businesses = new ArrayList<>();

  /** 기관의 작업 담당자 연락처. 한 명 이상이고 적은 순서대로다. */
  @OneToMany(mappedBy = "suspension", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<SuspensionContact> contacts = new ArrayList<>();

  @OneToMany(mappedBy = "suspension", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<SuspensionReviewer> reviewers = new ArrayList<>();

  protected Suspension() {}

  /**
   * 등록 양식의 내용. 기관은 등록할 때 정하고 바꾸지 않는다.
   *
   * @param targetWorks 고른 대상 업무. 하나 이상이고 기준정보 순서다
   * @param businesses 관련 레일 업무(대상 업무의 관련 업무를 모은 것). 비어 있을 수 있다
   * @param contacts 기관 작업 담당자 연락처. 한 명 이상이고 적은 순서다
   */
  public record Content(
      List<TargetWork> targetWorks,
      Set<Business> businesses,
      LocalDateTime startAt,
      LocalDateTime endAt,
      String reason,
      String kftcAction,
      List<ContactInfo> contacts,
      String extraNote) {}

  /** 담당자 연락처 한 명. 이메일은 없을 수 있다. */
  public record ContactInfo(String name, String phone, String email) {}

  /** 검토 대기로 등록한다. */
  public static Suspension register(
      Organization organization,
      Content content,
      String registrantEmail,
      String registrantName,
      String registrantJobTitle,
      boolean registrantKftc) {
    Suspension suspension = new Suspension();
    suspension.organization = organization;
    suspension.apply(content);
    suspension.status = SuspensionStatus.PENDING_REVIEW;
    suspension.registrantEmail = registrantEmail;
    suspension.registrantName = registrantName;
    suspension.registrantJobTitle = registrantJobTitle;
    suspension.registrantKftc = registrantKftc;
    return suspension;
  }

  /**
   * 내용을 고친다. 완료한 건은 고칠 수 없다.
   *
   * @throws IllegalStateException 완료면
   */
  public void change(Content content) {
    ensureNotCompleted();
    apply(content);
  }

  private void apply(Content content) {
    applyTargetWorks(content.targetWorks());
    applyBusinesses(content.businesses());
    applyContacts(content.contacts());
    this.startAt = content.startAt();
    this.endAt = content.endAt();
    this.reason = content.reason();
    this.kftcAction = content.kftcAction();
    this.extraNote = content.extraNote();
  }

  /** 빠진 대상 업무는 지우고 새 대상 업무를 더한다. 그대로인 행(고를 때의 이름)은 둔다. */
  private void applyTargetWorks(List<TargetWork> next) {
    List<Long> nextIds = next.stream().map(TargetWork::getId).toList();
    targetWorks.removeIf(w -> !nextIds.contains(w.getTargetWork().getId()));
    List<Long> have = targetWorks.stream().map(w -> w.getTargetWork().getId()).toList();
    next.stream()
        .filter(w -> !have.contains(w.getId()))
        .forEach(w -> targetWorks.add(new SuspensionTargetWork(this, w)));
  }

  /** 빠진 업무는 지우고 새 업무를 더한다. 그대로인 업무 행은 둔다. */
  private void applyBusinesses(Set<Business> next) {
    businesses.removeIf(b -> !next.contains(b.getBusiness()));
    Set<Business> have = getBusinesses();
    next.stream()
        .filter(b -> !have.contains(b))
        .forEach(b -> businesses.add(new SuspensionBusiness(this, b)));
  }

  /** 앞에서부터 있는 행을 고쳐 쓰고, 남는 행은 지우고 모자라면 더한다. */
  private void applyContacts(List<ContactInfo> next) {
    List<SuspensionContact> ordered = getContacts();
    for (int i = 0; i < next.size(); i++) {
      if (i < ordered.size()) {
        ordered.get(i).apply(next.get(i));
      } else {
        contacts.add(new SuspensionContact(this, i, next.get(i)));
      }
    }
    for (int i = next.size(); i < ordered.size(); i++) {
      contacts.remove(ordered.get(i));
    }
  }

  /** 다시 검토 대기로 돌린다. 기관이 내용을 고쳤을 때 결제원이 다시 확인하도록 확인 기록을 지운다. 쓴 조치·인수인계 사항은 남긴다. */
  public void reopenReview() {
    ensureNotCompleted();
    reviewers.clear();
    this.status = SuspensionStatus.PENDING_REVIEW;
    this.confirmedAt = null;
    this.confirmedByEmail = null;
    this.confirmedByName = null;
    this.confirmedByJobTitle = null;
  }

  /**
   * 결제원 담당자가 확인을 시작한다. 이미 시작한 사람이면 아무것도 바뀌지 않는다. 검토 대기면 검토 중이 된다.
   *
   * @return 새로 시작했으면 참
   */
  public boolean startReview(String email, String name, String roleLabel, LocalDateTime now) {
    ensureNotCompleted();
    if (isReviewer(email)) {
      return false;
    }
    reviewers.add(new SuspensionReviewer(this, email, name, roleLabel, now));
    if (status == SuspensionStatus.PENDING_REVIEW) {
      this.status = SuspensionStatus.IN_REVIEW;
    }
    return true;
  }

  /** 조치·인수인계 사항을 쓰고 확정한다. 확정한 뒤 다시 부르면 사항만 바꾼다. 확인을 시작하지 않은 사람이면 확인자로도 남긴다. */
  public void confirm(
      String handover,
      String email,
      String name,
      String jobTitle,
      String roleLabel,
      LocalDateTime now) {
    ensureNotCompleted();
    startReview(email, name, roleLabel, now);
    this.handover = handover;
    this.status = SuspensionStatus.CONFIRMED;
    this.confirmedAt = now;
    this.confirmedByEmail = email;
    this.confirmedByName = name;
    this.confirmedByJobTitle = jobTitle;
  }

  /** 종료 시각이 지난 확정 건을 완료한다. */
  public void complete(LocalDateTime now) {
    if (status != SuspensionStatus.CONFIRMED) {
      throw new IllegalStateException("확정한 건만 완료할 수 있습니다: " + status);
    }
    this.status = SuspensionStatus.COMPLETED;
    this.completedAt = now;
  }

  public boolean isReviewer(String email) {
    return reviewers.stream().anyMatch(r -> r.getEmail().equals(email));
  }

  /** 지금 기준 시간 구분. */
  public SuspensionPeriod periodAt(LocalDateTime now) {
    if (now.isBefore(startAt)) {
      return SuspensionPeriod.UPCOMING;
    }
    return now.isBefore(endAt) ? SuspensionPeriod.CURRENT : SuspensionPeriod.PAST;
  }

  private void ensureNotCompleted() {
    if (status == SuspensionStatus.COMPLETED) {
      throw new IllegalStateException("완료한 일시중단은 바꿀 수 없습니다.");
    }
  }

  public Content content() {
    return new Content(
        getTargetWorks().stream().map(SuspensionTargetWork::getTargetWork).toList(),
        getBusinesses(),
        startAt,
        endAt,
        reason,
        kftcAction,
        getContacts().stream().map(SuspensionContact::info).toList(),
        extraNote);
  }

  public Long getId() {
    return id;
  }

  /** 멈추는 업무. 업무 순서다. */
  public Set<Business> getBusinesses() {
    EnumSet<Business> result = EnumSet.noneOf(Business.class);
    businesses.forEach(b -> result.add(b.getBusiness()));
    return result;
  }

  /** 대표 업무(업무 순서로 첫째). 알림·이력처럼 업무 하나를 붙여야 하는 곳에 쓴다. 관련 레일 업무가 없으면 null. */
  public Business getPrimaryBusiness() {
    Set<Business> all = getBusinesses();
    return all.isEmpty() ? null : all.iterator().next();
  }

  /** 고른 대상 업무. 기준정보 순서다. */
  public List<SuspensionTargetWork> getTargetWorks() {
    return targetWorks.stream()
        .sorted(
            Comparator.comparingInt((SuspensionTargetWork w) -> w.getTargetWork().getSortOrder())
                .thenComparing(SuspensionTargetWork::getName))
        .toList();
  }

  /** 고른 대상 업무 이름을 "타행환, CMS" 꼴로. */
  public String targetWorkNames() {
    return String.join(", ", getTargetWorks().stream().map(SuspensionTargetWork::getName).toList());
  }

  public boolean affects(Business business) {
    return businesses.stream().anyMatch(b -> b.getBusiness() == business);
  }

  public Organization getOrganization() {
    return organization;
  }

  public LocalDateTime getStartAt() {
    return startAt;
  }

  public LocalDateTime getEndAt() {
    return endAt;
  }

  public String getReason() {
    return reason;
  }

  public String getKftcAction() {
    return kftcAction;
  }

  /** 담당자 연락처. 적은 순서다. */
  public List<SuspensionContact> getContacts() {
    return contacts.stream()
        .sorted(Comparator.comparingInt(SuspensionContact::getSortOrder))
        .toList();
  }

  public String getExtraNote() {
    return extraNote;
  }

  public SuspensionStatus getStatus() {
    return status;
  }

  public String getRegistrantEmail() {
    return registrantEmail;
  }

  public String getRegistrantName() {
    return registrantName;
  }

  public String getRegistrantJobTitle() {
    return registrantJobTitle;
  }

  public boolean isRegistrantKftc() {
    return registrantKftc;
  }

  public String getHandover() {
    return handover;
  }

  public LocalDateTime getConfirmedAt() {
    return confirmedAt;
  }

  public String getConfirmedByName() {
    return confirmedByName;
  }

  public String getConfirmedByJobTitle() {
    return confirmedByJobTitle;
  }

  public String getConfirmedByEmail() {
    return confirmedByEmail;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  /** 확인을 시작한 결제원 담당자. 시작한 순서다. */
  public List<SuspensionReviewer> getReviewers() {
    return reviewers.stream()
        .sorted(
            Comparator.comparing(SuspensionReviewer::getStartedAt)
                .thenComparing(SuspensionReviewer::getEmail))
        .toList();
  }
}
