package com.connectlink.testplan;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

/**
 * 대상 기관 하나가 요구사항 하나에 등록한 결과와, 그 결과에 대한 결제원 의견(미니 수행률·특이사항). 결제원 의견은 기관 결과를 덮어쓰지 않고 옆에 둔다.
 *
 * <p>결제원 의견 아래에는 결제원 담당자와 그 기관 담당자가 답글({@link TestItemComment})을 시간순으로 이어서 단다.
 *
 * <p>결제원 의견을 남긴 뒤 기관이 결과를 다시 등록하면 그 의견은 지난 의견이 되어(다시 확인 필요) 수행률·완료 계산에 쓰지 않는다.
 */
@Entity
@Table(name = "test_item_result")
public class TestItemResult extends AuditedEntity {

  /** 수행 완료로 보는 결제원 미니 수행률. */
  public static final int FULL_RATE = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_item_id")
  private TestItem item;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_plan_target_id")
  private TestPlanTarget target;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false, length = 20)
  private TestResult result;

  /** 특이사항. 오류면 오류내용이다. */
  @Column(name = "note", length = 2000)
  private String note;

  @Column(name = "reported_by_email", nullable = false, length = 200)
  private String reportedByEmail;

  @Column(name = "reported_by_name", nullable = false, length = 50)
  private String reportedByName;

  /** 등록한 기관 담당자의 그때 직급. 없으면 null. */
  @Column(name = "reported_by_job_title", length = 50)
  private String reportedByJobTitle;

  @Column(name = "reported_at", nullable = false)
  private LocalDateTime reportedAt;

  /** 결제원 미니 수행률(0~100). 100 이면 결제원도 수행완료로 본다. */
  @Column(name = "feedback_rate")
  private Integer feedbackRate;

  /** 결제원 특이사항(예: 003 전문에 대해 예외사항 발생). */
  @Column(name = "feedback_note", length = 2000)
  private String feedbackNote;

  @Column(name = "feedback_by_email", length = 200)
  private String feedbackByEmail;

  @Column(name = "feedback_by_name", length = 50)
  private String feedbackByName;

  /** 의견을 남긴 결제원 담당자의 그때 직급. 없으면 null. */
  @Column(name = "feedback_by_job_title", length = 50)
  private String feedbackByJobTitle;

  @Column(name = "feedback_at")
  private LocalDateTime feedbackAt;

  /** 결제원 의견에 이어 단 답글. 쓴 순서다. */
  @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("writtenAt ASC, id ASC")
  @BatchSize(size = 50)
  private List<TestItemComment> comments = new ArrayList<>();

  protected TestItemResult() {}

  TestItemResult(
      TestItem item,
      TestPlanTarget target,
      TestResult result,
      String note,
      String email,
      String name,
      String jobTitle,
      LocalDateTime now) {
    this.item = item;
    this.target = target;
    this.result = result;
    this.note = note;
    this.reportedByEmail = email;
    this.reportedByName = name;
    this.reportedByJobTitle = jobTitle;
    this.reportedAt = now;
  }

  /**
   * 기관이 결과를 다시 등록한다. 결과나 특이사항이 같으면 아무것도 바뀌지 않는다.
   *
   * @return 바뀌었으면 참
   */
  boolean report(
      TestResult result,
      String note,
      String email,
      String name,
      String jobTitle,
      LocalDateTime now) {
    if (this.result == result && Objects.equals(this.note, note)) {
      return false;
    }
    this.result = result;
    this.note = note;
    this.reportedByEmail = email;
    this.reportedByName = name;
    this.reportedByJobTitle = jobTitle;
    this.reportedAt = now;
    return true;
  }

  /** 결제원 의견을 남긴다. 미니 수행률과 특이사항이 모두 비면 의견을 지운다. */
  void giveFeedback(
      Integer rate, String note, String email, String name, String jobTitle, LocalDateTime now) {
    if (rate == null && note == null) {
      this.feedbackRate = null;
      this.feedbackNote = null;
      this.feedbackByEmail = null;
      this.feedbackByName = null;
      this.feedbackByJobTitle = null;
      this.feedbackAt = null;
      return;
    }
    this.feedbackRate = rate;
    this.feedbackNote = note;
    this.feedbackByEmail = email;
    this.feedbackByName = name;
    this.feedbackByJobTitle = jobTitle;
    this.feedbackAt = now;
  }

  TestItemComment addComment(TestItemComment comment) {
    comments.add(comment);
    return comment;
  }

  Optional<TestItemComment> findComment(Long commentId) {
    return comments.stream().filter(c -> c.getId().equals(commentId)).findFirst();
  }

  void removeComment(TestItemComment comment) {
    comments.remove(comment);
  }

  /** 결제원 의견이 있고 그 뒤에 기관이 결과를 다시 등록하지 않았는지. */
  public boolean hasCurrentFeedback() {
    return feedbackAt != null && !reportedAt.isAfter(feedbackAt);
  }

  /** 결제원 의견 뒤에 기관이 결과를 다시 등록해 결제원이 다시 확인해야 하는지. */
  public boolean isFeedbackOutdated() {
    return feedbackAt != null && reportedAt.isAfter(feedbackAt);
  }

  /** 이 요구사항의 수행률(0~100). 지금 유효한 결제원 미니 수행률이 있으면 그 값, 없으면 기관 결과로 정상 100, 그 밖 0 이다. */
  public int effectiveRate() {
    if (hasCurrentFeedback() && feedbackRate != null) {
      return feedbackRate;
    }
    return result == TestResult.PASSED ? FULL_RATE : 0;
  }

  /** 기관도 결제원도 수행완료로 표시했는지(기관 정상, 지금 유효한 결제원 미니 수행률 100). */
  public boolean isCompleted() {
    return result == TestResult.PASSED
        && hasCurrentFeedback()
        && feedbackRate != null
        && feedbackRate == FULL_RATE;
  }

  public Long getId() {
    return id;
  }

  public TestItem getItem() {
    return item;
  }

  public TestPlanTarget getTarget() {
    return target;
  }

  public TestResult getResult() {
    return result;
  }

  public String getNote() {
    return note;
  }

  public String getReportedByEmail() {
    return reportedByEmail;
  }

  public String getReportedByName() {
    return reportedByName;
  }

  public String getReportedByJobTitle() {
    return reportedByJobTitle;
  }

  public LocalDateTime getReportedAt() {
    return reportedAt;
  }

  public Integer getFeedbackRate() {
    return feedbackRate;
  }

  public String getFeedbackNote() {
    return feedbackNote;
  }

  public String getFeedbackByEmail() {
    return feedbackByEmail;
  }

  public String getFeedbackByName() {
    return feedbackByName;
  }

  public String getFeedbackByJobTitle() {
    return feedbackByJobTitle;
  }

  public LocalDateTime getFeedbackAt() {
    return feedbackAt;
  }

  public List<TestItemComment> getComments() {
    return List.copyOf(comments);
  }
}
