package com.connectlink.attachment.dto;

import com.connectlink.attachment.AttachmentApprovalStatus;
import com.connectlink.attachment.AttachmentOwnerType;
import java.time.LocalDateTime;

/**
 * 파일 결재함의 한 줄. 결제원 담당자가 올린 파일 하나와 그 반출 결재.
 *
 * @param ownerTitle 파일이 붙은 대상 이름(예: 공지 제목)
 * @param ownerBusiness 대상 업무의 slug. 업무가 없는 대상이면 null 이다
 * @param ownerPath 업무 영역 아래 대상 화면 경로(예: "notices?id=3"). 없으면 null 이다
 * @param uploaderTeam 올린 사람의 부서·팀
 */
public record AttachmentApprovalResponse(
    Long id,
    String name,
    String contentType,
    long size,
    AttachmentOwnerType ownerType,
    String ownerTypeLabel,
    Long ownerId,
    String ownerTitle,
    String ownerBusiness,
    String ownerPath,
    String uploaderEmail,
    String uploaderName,
    String uploaderTeam,
    LocalDateTime uploadedAt,
    AttachmentApprovalStatus status,
    String approverName,
    LocalDateTime decidedAt,
    String rejectionReason) {}
