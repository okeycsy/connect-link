package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
import com.connectlink.opinion.OpinionReplyAnswer.Stage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * 기관 회신. 대상 기관 한 곳에 하나다. 회신 내용은 요청 질문에 대한 답({@link OpinionReplyAnswer})뿐이고, 제출한 답과 임시저장한 답을 나눠 둔다.
 * 그래서 수정 중에 임시저장해도 결제원이 보는 제출 내용은 다시 제출할 때까지 그대로다. 한 번이라도 제출했으면 회신한 기관이다.
 */
@Entity
@Table(name = "opinion_reply")
public class OpinionReply extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "target_id", nullable = false, unique = true)
  private OpinionTarget target;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private OpinionReplyStatus status;

  /** 마지막으로 저장·제출한 기관 담당자. */
  @Column(name = "replier_email", length = 200)
  private String replierEmail;

  @Column(name = "replier_name", length = 50)
  private String replierName;

  /** 마지막 제출 일시. 제출 전이면 null. */
  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "revision_reason", length = 500)
  private String revisionReason;

  @Column(name = "revision_requested_at")
  private LocalDateTime revisionRequestedAt;

  /** 결제원 담당자가 수정 요청을 받아들이지 않은 사유. 다음 수정 요청이나 제출 때 지운다. */
  @Column(name = "revision_rejection_reason", length = 500)
  private String revisionRejectionReason;

  @OneToMany(mappedBy = "reply", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OpinionReplyAnswer> answers = new ArrayList<>();

  protected OpinionReply() {}

  OpinionReply(OpinionTarget target) {
    this.target = target;
    this.status = OpinionReplyStatus.DRAFT;
  }

  /** 지금 고칠 수 있는지. 처음 제출 전(임시저장)과 수정 요청이 승인된 뒤(수정 중)다. */
  public boolean isEditable() {
    return status == OpinionReplyStatus.DRAFT || status == OpinionReplyStatus.REVISING;
  }

  /** 한 번이라도 제출했는지. 회신 여부의 기준이다. */
  public boolean isReplied() {
    return submittedAt != null;
  }

  /**
   * 임시저장. 제출 내용은 그대로 둔다.
   *
   * @throws IllegalStateException 고칠 수 없는 상태면
   */
  void saveDraft(List<Answer> answers, String email, String name) {
    ensureEditable();
    replaceAnswers(Stage.DRAFT, answers);
    this.replierEmail = email;
    this.replierName = name;
  }

  /** 최종제출. 임시저장한 내용은 비운다. */
  void submit(List<Answer> answers, String email, String name, LocalDateTime now) {
    ensureEditable();
    replaceAnswers(Stage.SUBMITTED, answers);
    replaceAnswers(Stage.DRAFT, List.of());
    this.replierEmail = email;
    this.replierName = name;
    this.submittedAt = now;
    this.status = OpinionReplyStatus.SUBMITTED;
    this.revisionReason = null;
    this.revisionRequestedAt = null;
    this.revisionRejectionReason = null;
  }

  /** 제출한 뒤 고치고 싶다고 결제원 담당자에게 요청한다. */
  void requestRevision(String reason, String email, String name, LocalDateTime now) {
    if (status != OpinionReplyStatus.SUBMITTED) {
      throw new IllegalStateException("제출한 회신만 수정 요청할 수 있습니다: " + status);
    }
    this.status = OpinionReplyStatus.REVISION_REQUESTED;
    this.revisionReason = reason;
    this.revisionRequestedAt = now;
    this.revisionRejectionReason = null;
    this.replierEmail = email;
    this.replierName = name;
  }

  /** 수정 요청 승인. 제출한 내용을 임시저장 칸에 옮겨 그 위에서 고치게 한다. */
  void approveRevision() {
    ensureRevisionRequested();
    this.status = OpinionReplyStatus.REVISING;
    replaceAnswers(Stage.DRAFT, getAnswers(Stage.SUBMITTED));
  }

  /** 수정 요청을 받아들이지 않는다. 제출 상태로 돌아간다. */
  void rejectRevision(String reason) {
    ensureRevisionRequested();
    this.status = OpinionReplyStatus.SUBMITTED;
    this.revisionRejectionReason = reason;
  }

  /** 요청이 마감되었다. 수정 요청·수정 중이던 회신은 제출한 내용으로 돌아간다. 제출하지 않은 회신은 임시저장 그대로 남는다. */
  void closeOut() {
    if (status == OpinionReplyStatus.REVISION_REQUESTED || status == OpinionReplyStatus.REVISING) {
      this.status = OpinionReplyStatus.SUBMITTED;
      replaceAnswers(Stage.DRAFT, List.of());
    }
  }

  /** 한 단계의 답을 보낸 목록대로 맞춘다. 같은 칸(질문·선택지)은 행을 그대로 두고 주관식 글만 바꾼다(지우고 다시 넣지 않는다). */
  private void replaceAnswers(Stage stage, List<Answer> values) {
    answers.removeIf(
        a ->
            a.getStage() == stage
                && values.stream().noneMatch(v -> v.sameCell(a.getQuestionId(), a.getOptionId())));
    for (Answer value : values) {
      answers.stream()
          .filter(a -> a.getStage() == stage && value.sameCell(a.getQuestionId(), a.getOptionId()))
          .findFirst()
          .ifPresentOrElse(
              a -> a.changeText(value.text()),
              () ->
                  answers.add(
                      new OpinionReplyAnswer(
                          this, stage, value.questionId(), value.optionId(), value.text())));
    }
  }

  /** 한 단계의 답. */
  public List<Answer> getAnswers(Stage stage) {
    return answers.stream()
        .filter(a -> a.getStage() == stage)
        .map(a -> new Answer(a.getQuestionId(), a.getOptionId(), a.getTextValue()))
        .toList();
  }

  /**
   * 답 한 칸의 값.
   *
   * @param optionId 객관식이면 고른 선택지, 주관식이면 null
   * @param text 주관식이면 답, 객관식이면 null
   */
  public record Answer(Long questionId, Long optionId, String text) {
    boolean sameCell(Long questionId, Long optionId) {
      return this.questionId.equals(questionId) && Objects.equals(this.optionId, optionId);
    }
  }

  private void ensureEditable() {
    if (!isEditable()) {
      throw new IllegalStateException("지금은 회신을 고칠 수 없습니다: " + status);
    }
  }

  private void ensureRevisionRequested() {
    if (status != OpinionReplyStatus.REVISION_REQUESTED) {
      throw new IllegalStateException("수정 요청한 회신이 아닙니다: " + status);
    }
  }

  public Long getId() {
    return id;
  }

  public OpinionTarget getTarget() {
    return target;
  }

  public OpinionReplyStatus getStatus() {
    return status;
  }

  public String getReplierEmail() {
    return replierEmail;
  }

  public String getReplierName() {
    return replierName;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public String getRevisionReason() {
    return revisionReason;
  }

  public LocalDateTime getRevisionRequestedAt() {
    return revisionRequestedAt;
  }

  public String getRevisionRejectionReason() {
    return revisionRejectionReason;
  }
}
