package com.connectlink.testplan;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Organization;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import javax.persistence.OrderBy;
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

  /** 테스트 전 테스트데이터 생성 여부. 파일은 첨부(TEST_DATA, 이 행의 ID)로 올린다. */
  @Enumerated(EnumType.STRING)
  @Column(name = "test_data_status", nullable = false, length = 20)
  private TestDataStatus testDataStatus = TestDataStatus.NOT_CREATED;

  /** 테스트데이터 메모(만든 데이터 설명, 못 만든 이유 등). */
  @Column(name = "test_data_note", length = 2000)
  private String testDataNote;

  @Column(name = "test_data_reported_by_email", length = 200)
  private String testDataReportedByEmail;

  @Column(name = "test_data_reported_by_name", length = 50)
  private String testDataReportedByName;

  /** 테스트데이터를 마지막으로 바꾼 기관 담당자의 그때 직급. 없으면 null. */
  @Column(name = "test_data_reported_by_job_title", length = 50)
  private String testDataReportedByJobTitle;

  /** 기관이 테스트데이터 생성 여부·메모를 마지막으로 바꾼 일시. 아직 없으면 null. */
  @Column(name = "test_data_reported_at")
  private LocalDateTime testDataReportedAt;

  @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<TestItemResult> results = new ArrayList<>();

  /** 테스트 담당자(고른 순서). 비면 그 기관의 업무 담당자 전원이 메일을 받는다. */
  @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  @BatchSize(size = 50)
  private List<TestPlanTargetContact> contacts = new ArrayList<>();

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

  /** 테스트데이터 생성 여부·메모를 바꾼다. 바뀐 것이 없으면 아무것도 하지 않고 false. */
  boolean reportTestData(
      TestDataStatus status,
      String note,
      String email,
      String name,
      String jobTitle,
      LocalDateTime now) {
    if (testDataStatus == status && Objects.equals(testDataNote, note)) {
      return false;
    }
    this.testDataStatus = status;
    this.testDataNote = note;
    this.testDataReportedByEmail = email;
    this.testDataReportedByName = name;
    this.testDataReportedByJobTitle = jobTitle;
    this.testDataReportedAt = now;
    return true;
  }

  /** 테스트 담당자를 요청한 사람들(이메일 → 고를 때 값)로 맞춘다. 이미 있는 사람은 그대로 둔다. */
  void replaceContacts(Map<String, TestPlanTargetContact.Snapshot> requested) {
    contacts.removeIf(c -> !requested.containsKey(c.getEmail()));
    requested.forEach(
        (email, snapshot) -> {
          if (contacts.stream().noneMatch(c -> c.getEmail().equals(email))) {
            contacts.add(new TestPlanTargetContact(this, snapshot));
          }
        });
  }

  /** 탈퇴한 사람을 테스트 담당자에서 뺀다. */
  void removeContact(String email) {
    contacts.removeIf(c -> c.getEmail().equals(email));
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

  public TestDataStatus getTestDataStatus() {
    return testDataStatus;
  }

  public String getTestDataNote() {
    return testDataNote;
  }

  public String getTestDataReportedByEmail() {
    return testDataReportedByEmail;
  }

  public String getTestDataReportedByName() {
    return testDataReportedByName;
  }

  public String getTestDataReportedByJobTitle() {
    return testDataReportedByJobTitle;
  }

  public LocalDateTime getTestDataReportedAt() {
    return testDataReportedAt;
  }

  public List<TestItemResult> getResults() {
    return List.copyOf(results);
  }

  public List<TestPlanTargetContact> getContacts() {
    return List.copyOf(contacts);
  }
}
