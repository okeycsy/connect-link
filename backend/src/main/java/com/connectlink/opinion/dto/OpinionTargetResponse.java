package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionReplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 의견 요청의 대상 기관 한 곳.
 *
 * @param formId 고른 회신 양식. 없으면 null
 * @param replied 한 번이라도 제출했는지
 * @param replyStatus 회신 상태. 아직 저장한 적이 없으면 null
 */
public record OpinionTargetResponse(
    Long id,
    Long organizationId,
    String organizationCode,
    String organizationName,
    @Schema(nullable = true) Long formId,
    @Schema(nullable = true) String formName,
    List<OpinionRecipientResponse> recipients,
    boolean replied,
    @Schema(nullable = true) OpinionReplyStatus replyStatus) {}
