package com.connectlink.testplan;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

/**
 * 기관 테스트 한 건(기관 테스트 수행·결과 관리 페이지). 결제원 담당자가 한 업무의 단위업무/개발 건마다 만들고, 대상 참가기관({@link TestPlanTarget})이
 * 요구사항({@link TestItem})마다 결과({@link TestItemResult})를 등록한다. 만들면 바로 대상 기관에 열린다.
 *
 * <p>수행률과 수행 완료는 결과가 바뀔 때마다 {@link TestProgress} 로 다시 계산해 둔다.
 */
@Entity
@Table(name = "test_plan")
public class TestPlan extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  /** 단위업무(세부업무). */
  @Column(name = "sub_business", nullable = false, length = 200)
  private String subBusiness;

  /** 개발 건 이름(테스트명). */
  @Column(name = "title", nullable = false, length = 200)
  private String title;

  /** 테스트 내용·수행 가이드. */
  @Column(name = "description", length = 4000)
  private String description;

  /** 수행 기간. 기간이 지나도 결과 등록은 막지 않는다. */
  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  /** 만든 결제원 담당자. */
  @Column(name = "owner_email", nullable = false, length = 200)
  private String ownerEmail;

  @Column(name = "owner_name", nullable = false, length = 50)
  private String ownerName;

  /** 전체 수행률(0~100). 대상 기관 × 요구사항 수행률의 평균이다. */
  @Column(name = "progress_rate", nullable = false)
  private int progressRate;

  /** 수행 완료 일시. 완료가 풀리면(요구사항·기관이 늘거나 결과가 바뀌면) 다시 비운다. */
  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<TestItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<TestPlanTarget> targets = new ArrayList<>();

  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<TestPlanRecipient> recipients = new ArrayList<>();

  protected TestPlan() {}

  /** 만들기·고치기 양식의 기본 정보. 업무는 만든 뒤 바꾸지 않는다. */
  public record Content(
      String subBusiness,
      String title,
      String description,
      LocalDate startDate,
      LocalDate endDate) {}

  /** 요구사항 하나의 내용. {@code id} 가 있으면 기존 요구사항을 고치고, 없으면 새로 넣는다. */
  public record ItemDraft(Long id, String title, String description) {}

  public static TestPlan create(
      Business business, Content content, String ownerEmail, String ownerName) {
    TestPlan plan = new TestPlan();
    plan.business = business;
    plan.apply(content);
    plan.ownerEmail = ownerEmail;
    plan.ownerName = ownerName;
    return plan;
  }

  public void change(Content content) {
    apply(content);
  }

  private void apply(Content content) {
    this.subBusiness = content.subBusiness();
    this.title = content.title();
    this.description = content.description();
    this.startDate = content.startDate();
    this.endDate = content.endDate();
  }

  /**
   * 요구사항을 목록대로 맞춘다. id 가 있는 것은 고치고 순서를 바꾸며, 없는 것은 새로 넣고, 목록에 없는 기존 요구사항은 뺀다. 결과가 있는 요구사항을 빼면 안 되는지는
   * Service 가 먼저 확인한다.
   *
   * @throws IllegalArgumentException 이 테스트에 없는 요구사항 id 면
   */
  void replaceItems(List<ItemDraft> drafts) {
    List<TestItem> kept = new ArrayList<>();
    for (int i = 0; i < drafts.size(); i++) {
      ItemDraft d = drafts.get(i);
      TestItem item;
      if (d.id() == null) {
        item = new TestItem(this, i, d.title(), d.description());
        items.add(item);
      } else {
        item =
            findItem(d.id())
                .orElseThrow(() -> new IllegalArgumentException("이 테스트의 요구사항이 아닙니다: " + d.id()));
        item.change(i, d.title(), d.description());
      }
      kept.add(item);
    }
    items.removeIf(item -> !kept.contains(item));
  }

  TestPlanTarget addTarget(Organization organization) {
    TestPlanTarget target = new TestPlanTarget(this, organization);
    targets.add(target);
    return target;
  }

  void removeTarget(TestPlanTarget target) {
    targets.remove(target);
  }

  /** 알림 받을 결제원 담당자를 목록대로 바꾼다. */
  void replaceRecipients(List<TestPlanRecipient.Draft> drafts) {
    recipients.removeIf(r -> drafts.stream().noneMatch(d -> d.email().equals(r.getEmail())));
    for (TestPlanRecipient.Draft d : drafts) {
      if (recipients.stream().noneMatch(r -> r.getEmail().equals(d.email()))) {
        recipients.add(new TestPlanRecipient(this, d.accountId(), d.email(), d.name()));
      }
    }
    touch();
  }

  /** 탈퇴한 계정을 알림 대상에서 뺀다. */
  void removeRecipient(Long accountId) {
    recipients.removeIf(r -> accountId.equals(r.getAccountId()));
  }

  /** 다시 계산한 수행률과 완료 여부를 반영한다. 이미 완료였으면 완료 일시를 그대로 둔다. */
  void updateProgress(int rate, boolean completed, LocalDateTime now) {
    this.progressRate = rate;
    if (!completed) {
      this.completedAt = null;
    } else if (this.completedAt == null) {
      this.completedAt = now;
    }
  }

  public TestPlanState getState() {
    return completedAt == null ? TestPlanState.IN_PROGRESS : TestPlanState.COMPLETED;
  }

  /** 수행 기간이 지났는데 아직 완료가 아닌지. */
  public boolean isOverdue(LocalDate today) {
    return completedAt == null && today.isAfter(endDate);
  }

  public Optional<TestItem> findItem(Long itemId) {
    return items.stream().filter(i -> itemId.equals(i.getId())).findFirst();
  }

  public Optional<TestPlanTarget> findTarget(Long organizationId) {
    return targets.stream()
        .filter(t -> t.getOrganization().getId().equals(organizationId))
        .findFirst();
  }

  public Optional<TestPlanTarget> findTargetByCode(String organizationCode) {
    return targets.stream()
        .filter(t -> t.getOrganization().getCode().equals(organizationCode))
        .findFirst();
  }

  public boolean isRecipient(String email) {
    return recipients.stream().anyMatch(r -> r.getEmail().equals(email));
  }

  public Long getId() {
    return id;
  }

  public Business getBusiness() {
    return business;
  }

  public String getSubBusiness() {
    return subBusiness;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public int getProgressRate() {
    return progressRate;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  /** 요구사항. 정한 순서다. */
  public List<TestItem> getItems() {
    return items.stream().sorted(Comparator.comparingInt(TestItem::getSortOrder)).toList();
  }

  /** 대상 기관. 기관 이름순이다. */
  public List<TestPlanTarget> getTargets() {
    return targets.stream()
        .sorted(Comparator.comparing(t -> t.getOrganization().getName()))
        .toList();
  }

  /** 알림 받을 결제원 담당자. 이름순이다. */
  public List<TestPlanRecipient> getRecipients() {
    return recipients.stream().sorted(Comparator.comparing(TestPlanRecipient::getName)).toList();
  }
}
