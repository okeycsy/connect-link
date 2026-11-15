package com.connectlink.attachment.dto;

import com.connectlink.attachment.AttachmentOwnerType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 결재 한 건. 결제원 담당자가 공지·의견 요청·기관 테스트 등을 등록하면서 붙인 파일들을 팀장이 한 번에 결재한다.
 *
 * @param ownerTitle 파일이 붙은 대상 이름(예: 공지 제목)
 * @param ownerBusiness 대상 업무의 slug. 업무가 없는 대상이면 null 이다
 * @param ownerPath 업무 영역 아래 대상 화면 경로(예: "notices?id=3"). 없으면 null 이다
 * @param files 내가 결재자인 파일(최근에 올린 것부터). 결재 대기가 없으면 이미 결재한 건이다
 */
public record AttachmentApprovalBatchResponse(
    AttachmentOwnerType ownerType,
    String ownerTypeLabel,
    Long ownerId,
    String ownerTitle,
    @Schema(nullable = true) String ownerBusiness,
    @Schema(nullable = true) String ownerPath,
    List<AttachmentApprovalResponse> files) {}
