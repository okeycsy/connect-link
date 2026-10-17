package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 질문 하나의 답.
 *
 * @param optionIds 객관식이면 고른 선택지 ID(하나 고르기는 한 개까지)
 * @param text 주관식이면 답
 */
public record OpinionAnswerRequest(
    @NotNull(message = "질문을 골라 주세요.") Long questionId,
    @Schema(nullable = true) List<Long> optionIds,
    @Schema(nullable = true) @Size(max = 4000, message = "답은 4000자까지입니다.") String text) {
  public OpinionAnswerRequest {
    optionIds = optionIds == null ? List.of() : List.copyOf(optionIds);
  }
}
