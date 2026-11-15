package com.connectlink.attachment;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 첨부파일 한 건. 파일 내용은 {@link FileStorage} 에 있고 여기에는 그 키와 원래 이름·종류·크기만 둔다. 올린 사람·일시는 감사 컬럼이다. */
@Entity
@Table(name = "attachment")
public class Attachment extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "owner_type", nullable = false, length = 30)
  private AttachmentOwnerType ownerType;

  @Column(name = "owner_id", nullable = false)
  private Long ownerId;

  /** 올릴 때의 파일 이름. 내려받을 때 이 이름을 돌려준다. */
  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  /** 저장소 안의 위치. 저장소 구현이 정한다. */
  @Column(name = "storage_key", nullable = false, length = 200)
  private String storageKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "approval_status", nullable = false, length = 20)
  private AttachmentApprovalStatus approvalStatus = AttachmentApprovalStatus.NOT_REQUIRED;

  /** 올린 사람 이름(결재함 표시용). 결재 절차 전에 올린 파일은 비어 있다. */
  @Column(name = "uploader_name", length = 50)
  private String uploaderName;

  /** 올린 사람의 부서·팀(결재함 표시용). */
  @Column(name = "uploader_team", length = 100)
  private String uploaderTeam;

  /** 결재자(같은 팀 팀장) 회사 이메일. 팀장 본인이 올렸으면 본인이다. */
  @Column(name = "approver_email", length = 200)
  private String approverEmail;

  @Column(name = "approver_name", length = 50)
  private String approverName;

  /** 승인·반려한 일시. */
  @Column(name = "decided_at")
  private LocalDateTime decidedAt;

  @Column(name = "rejection_reason", length = 500)
  private String rejectionReason;

  protected Attachment() {}

  public Attachment(
      AttachmentOwnerType ownerType,
      Long ownerId,
      String originalName,
      String contentType,
      long sizeBytes,
      String storageKey) {
    this.ownerType = ownerType;
    this.ownerId = ownerId;
    this.originalName = originalName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.storageKey = storageKey;
  }

  public Long getId() {
    return id;
  }

  public AttachmentOwnerType getOwnerType() {
    return ownerType;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getStorageKey() {
    return storageKey;
  }

  /**
   * 결제원 담당자가 올린 파일의 결재를 정한다. 결재자가 올린 사람 본인(팀장)이면 바로 승인이다.
   *
   * @param approverEmail 같은 팀 팀장의 회사 이메일
   */
  void requireApproval(
      String uploaderEmail,
      String uploaderName,
      String uploaderTeam,
      String approverEmail,
      String approverName,
      LocalDateTime now) {
    this.uploaderName = uploaderName;
    this.uploaderTeam = uploaderTeam;
    this.approverEmail = approverEmail;
    this.approverName = approverName;
    if (approverEmail.equals(uploaderEmail)) {
      this.approvalStatus = AttachmentApprovalStatus.APPROVED;
      this.decidedAt = now;
    } else {
      this.approvalStatus = AttachmentApprovalStatus.PENDING;
    }
  }

  /** 결재 대기 파일을 승인한다. 그 뒤 참가기관에 보인다. */
  void approve(LocalDateTime now) {
    this.approvalStatus = AttachmentApprovalStatus.APPROVED;
    this.decidedAt = now;
    this.rejectionReason = null;
  }

  /** 결재 대기 파일을 반려한다. 결제원 계정에만 보이고, 올린 사람이 지우고 다시 올린다. */
  void reject(String reason, LocalDateTime now) {
    this.approvalStatus = AttachmentApprovalStatus.REJECTED;
    this.decidedAt = now;
    this.rejectionReason = reason;
  }

  public AttachmentApprovalStatus getApprovalStatus() {
    return approvalStatus;
  }

  public String getUploaderName() {
    return uploaderName;
  }

  public String getUploaderTeam() {
    return uploaderTeam;
  }

  public String getApproverEmail() {
    return approverEmail;
  }

  public String getApproverName() {
    return approverName;
  }

  public LocalDateTime getDecidedAt() {
    return decidedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  /** 샘플 첨부의 내용을 저장소에 넣은 뒤 키와 크기를 맞춘다. 샘플 시드({@code SampleAttachmentLoader})만 쓴다. */
  public void replaceContent(String storageKey, long sizeBytes) {
    this.storageKey = storageKey;
    this.sizeBytes = sizeBytes;
  }
}
