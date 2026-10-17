package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * 기관 회신. 대상 기관 한 곳에 하나다. 제출한 내용({@code type}, {@code content})과 임시저장한 내용({@code draftType}, {@code
 * draftContent})을 따로 둔다. 그래서 수정 중에 임시저장해도 결제원이 보는 제출 내용은 다시 제출할 때까지 그대로다. 한 번이라도 제출했으면 회신한 기관이다.
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

  @Enumerated(EnumType.STRING)
  @Column(name = "reply_type", length = 20)
  private OpinionReplyType type;

  /** 제출한 세부의견. */
  @Column(name = "content", length = 4000)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "draft_type", length = 20)
  private OpinionReplyType draftType;

  @Column(name = "draft_content", length = 4000)
  private String draftContent;

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
  void saveDraft(OpinionReplyType type, String content, String email, String name) {
    ensureEditable();
    this.draftType = type;
    this.draftContent = content;
    this.replierEmail = email;
    this.replierName = name;
  }

  /** 최종제출. 임시저장한 내용은 비운다. */
  void submit(OpinionReplyType type, String content, String email, String name, LocalDateTime now) {
    ensureEditable();
    this.type = type;
    this.content = content;
    this.draftType = null;
    this.draftContent = null;
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
    this.draftType = type;
    this.draftContent = content;
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
      this.draftType = null;
      this.draftContent = null;
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

  public OpinionReplyType getType() {
    return type;
  }

  public String getContent() {
    return content;
  }

  public OpinionReplyType getDraftType() {
    return draftType;
  }

  public String getDraftContent() {
    return draftContent;
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
