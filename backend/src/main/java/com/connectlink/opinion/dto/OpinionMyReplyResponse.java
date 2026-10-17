package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionReplyStatus;
import com.connectlink.opinion.OpinionReplyType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 기관 담당자의 회신 화면. 제출한 내용과 임시저장한 내용을 따로 준다.
 *
 * @param replyId 회신 ID. 아직 저장한 적이 없으면 null. 근거자료는 첨부 API(ownerType=OPINION_REPLY, ownerId=replyId)
 * @param form 이 기관의 회신 양식. 없으면 null
 * @param status 회신 상태. 저장한 적이 없으면 null
 * @param type 제출한 의견 유형
 * @param draftType 임시저장한 의견 유형. 처음 쓰는 중이거나 수정 중일 때 있다
 * @param editable 지금 임시저장·제출할 수 있는지
 * @param revisionRequestable 제출한 뒤 수정 요청할 수 있는지
 */
public record OpinionMyReplyResponse(
    Long requestId,
    Long targetId,
    String organizationName,
    @Schema(nullable = true) OpinionReplyFormResponse form,
    @Schema(nullable = true) Long replyId,
    @Schema(nullable = true) OpinionReplyStatus status,
    String statusLabel,
    @Schema(nullable = true) OpinionReplyType type,
    @Schema(nullable = true) String typeLabel,
    @Schema(nullable = true) String content,
    @Schema(nullable = true) OpinionReplyType draftType,
    @Schema(nullable = true) String draftContent,
    @Schema(nullable = true) String replierName,
    @Schema(nullable = true) LocalDateTime submittedAt,
    @Schema(nullable = true) String revisionReason,
    @Schema(nullable = true) LocalDateTime revisionRequestedAt,
    @Schema(nullable = true) String revisionRejectionReason,
    boolean editable,
    boolean revisionRequestable) {}
