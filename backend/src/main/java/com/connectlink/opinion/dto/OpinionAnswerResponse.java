package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 질문 하나의 답.
 *
 * @param optionIds 객관식이면 고른 선택지 ID
 * @param text 주관식이면 답
 */
public record OpinionAnswerResponse(
    Long questionId, List<Long> optionIds, @Schema(nullable = true) String text) {}
