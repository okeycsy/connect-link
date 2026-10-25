package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionReplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기관 담당자의 회신 화면. 제출한 답과 임시저장한 답을 따로 준다.
 *
 * @param replyId 회신 ID. 아직 저장한 적이 없으면 null. 근거자료는 첨부 API(ownerType=OPINION_REPLY, ownerId=replyId)
 * @param status 회신 상태. 저장한 적이 없으면 null
 * @param answers 제출한 질문 답
 * @param draftAnswers 임시저장한 질문 답
 * @param editable 지금 임시저장·제출할 수 있는지
 * @param revisionRequestable 제출한 뒤 수정 요청할 수 있는지
 */
public record OpinionMyReplyResponse(
    Long requestId,
    Long targetId,
    String organizationCode,
    String organizationName,
    @Schema(nullable = true) Long replyId,
    @Schema(nullable = true) OpinionReplyStatus status,
    String statusLabel,
    List<OpinionAnswerResponse> answers,
    List<OpinionAnswerResponse> draftAnswers,
    @Schema(nullable = true) String replierName,
    @Schema(nullable = true) LocalDateTime submittedAt,
    @Schema(nullable = true) String revisionReason,
    @Schema(nullable = true) LocalDateTime revisionRequestedAt,
    @Schema(nullable = true) String revisionRejectionReason,
    boolean editable,
    boolean revisionRequestable) {}
