package com.connectlink.attachment.dto;

import com.connectlink.attachment.AttachmentApprovalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 결재 한 건 안의 파일 하나와 그 반출 결재.
 *
 * @param uploaderTeam 올린 사람의 부서·팀
 */
public record AttachmentApprovalResponse(
    Long id,
    String name,
    String contentType,
    long size,
    String uploaderEmail,
    @Schema(nullable = true) String uploaderName,
    @Schema(nullable = true) String uploaderTeam,
    LocalDateTime uploadedAt,
    AttachmentApprovalStatus status,
    @Schema(nullable = true) String approverName,
    @Schema(nullable = true) LocalDateTime decidedAt,
    @Schema(nullable = true) String rejectionReason) {}
