package com.connectlink.testplan;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Organization;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/** 테스트 대상 참가기관 한 곳과 그 기관의 요구사항별 결과. 기관 수행률·완료 요구사항 수는 결과가 바뀔 때마다 다시 계산해 둔다. */
@Entity
@Table(name = "test_plan_target")
public class TestPlanTarget extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_plan_id")
  private TestPlan plan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  /** 이 기관의 수행률(0~100). */
  @Column(name = "progress_rate", nullable = false)
  private int progressRate;

  /** 완료한 요구사항 수(기관 정상 + 결제원 100%). */
  @Column(name = "completed_count", nullable = false)
  private int completedCount;

  /** 이 기관이 마지막으로 결과를 등록한 일시. 아직 없으면 null. */
  @Column(name = "last_reported_at")
  private LocalDateTime lastReportedAt;

  @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<TestItemResult> results = new ArrayList<>();

  protected TestPlanTarget() {}

  TestPlanTarget(TestPlan plan, Organization organization) {
    this.plan = plan;
    this.organization = organization;
  }

  /** 요구사항 하나의 결과. 아직 등록하지 않았으면 비어 있다. */
  public Optional<TestItemResult> findResult(Long itemId) {
    return results.stream().filter(r -> r.getItem().getId().equals(itemId)).findFirst();
  }

  TestItemResult addResult(TestItemResult result) {
    results.add(result);
    return result;
  }

  void reported(LocalDateTime now) {
    this.lastReportedAt = now;
  }

  void updateProgress(int rate, int completed) {
    this.progressRate = rate;
    this.completedCount = completed;
  }

  /** 결과를 모두 지운다. 테스트를 지울 때 요구사항보다 먼저 지우려고 쓴다. */
  void clearResults() {
    results.clear();
  }

  /** 요구사항 하나에 등록한 결과가 있는지. */
  public boolean hasResult(Long itemId) {
    return findResult(itemId).isPresent();
  }

  public Long getId() {
    return id;
  }

  public TestPlan getPlan() {
    return plan;
  }

  public Organization getOrganization() {
    return organization;
  }

  public int getProgressRate() {
    return progressRate;
  }

  public int getCompletedCount() {
    return completedCount;
  }

  public LocalDateTime getLastReportedAt() {
    return lastReportedAt;
  }

  public List<TestItemResult> getResults() {
    return List.copyOf(results);
  }
}
