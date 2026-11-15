package com.connectlink.attachment.dto;

import com.connectlink.attachment.Attachment;
import com.connectlink.attachment.AttachmentApprovalStatus;
import com.connectlink.attachment.AttachmentOwnerType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 첨부파일 한 건(화면용). 내려받기 주소는 {@code /api/v1/attachments/{id}/download} 다.
 *
 * @param uploaderName 올린 사람 이름. 결재 절차 전에 올린 파일은 null 이다
 * @param approvalStatus 반출 결재 상태. 결재 대기·반려 파일은 결제원 계정에게만 내려간다
 * @param approverName 결재자(같은 팀 팀장) 이름. 결재가 필요 없는 파일은 null 이다
 */
public record AttachmentResponse(
    Long id,
    AttachmentOwnerType ownerType,
    Long ownerId,
    String name,
    String contentType,
    long size,
    String uploadedBy,
    @Schema(nullable = true) String uploaderName,
    LocalDateTime uploadedAt,
    AttachmentApprovalStatus approvalStatus,
    @Schema(nullable = true) String approverName,
    @Schema(nullable = true) LocalDateTime decidedAt,
    @Schema(nullable = true) String rejectionReason) {

  public static AttachmentResponse from(Attachment attachment) {
    return new AttachmentResponse(
        attachment.getId(),
        attachment.getOwnerType(),
        attachment.getOwnerId(),
        attachment.getOriginalName(),
        attachment.getContentType(),
        attachment.getSizeBytes(),
        attachment.getCreatedBy(),
        attachment.getUploaderName(),
        attachment.getCreatedAt(),
        attachment.getApprovalStatus(),
        attachment.getApproverName(),
        attachment.getDecidedAt(),
        attachment.getRejectionReason());
  }
}
