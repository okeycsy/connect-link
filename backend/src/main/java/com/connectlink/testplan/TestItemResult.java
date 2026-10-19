package com.connectlink.testplan;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import java.util.Objects;
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
import javax.persistence.Table;

/**
 * 대상 기관 하나가 요구사항 하나에 등록한 결과와, 그 결과에 대한 결제원 의견(미니 수행률·특이사항). 결제원 의견은 기관 결과를 덮어쓰지 않고 옆에 둔다.
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

  @Column(name = "feedback_at")
  private LocalDateTime feedbackAt;

  protected TestItemResult() {}

  TestItemResult(
      TestItem item,
      TestPlanTarget target,
      TestResult result,
      String note,
      String email,
      String name,
      LocalDateTime now) {
    this.item = item;
    this.target = target;
    this.result = result;
    this.note = note;
    this.reportedByEmail = email;
    this.reportedByName = name;
    this.reportedAt = now;
  }

  /**
   * 기관이 결과를 다시 등록한다. 결과나 특이사항이 같으면 아무것도 바뀌지 않는다.
   *
   * @return 바뀌었으면 참
   */
  boolean report(TestResult result, String note, String email, String name, LocalDateTime now) {
    if (this.result == result && Objects.equals(this.note, note)) {
      return false;
    }
    this.result = result;
    this.note = note;
    this.reportedByEmail = email;
    this.reportedByName = name;
    this.reportedAt = now;
    return true;
  }

  /** 결제원 의견을 남긴다. 미니 수행률과 특이사항이 모두 비면 의견을 지운다. */
  void giveFeedback(Integer rate, String note, String email, String name, LocalDateTime now) {
    if (rate == null && note == null) {
      this.feedbackRate = null;
      this.feedbackNote = null;
      this.feedbackByEmail = null;
      this.feedbackByName = null;
      this.feedbackAt = null;
      return;
    }
    this.feedbackRate = rate;
    this.feedbackNote = note;
    this.feedbackByEmail = email;
    this.feedbackByName = name;
    this.feedbackAt = now;
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

  public LocalDateTime getFeedbackAt() {
    return feedbackAt;
  }
}
