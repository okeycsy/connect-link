package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 질문 하나.
 *
 * @param options 선택지(정한 순서). 주관식이면 비어 있다
 */
public record OpinionQuestionResponse(
    Long id,
    OpinionQuestionType type,
    String typeLabel,
    String title,
    boolean required,
    List<Option> options) {

  @Schema(name = "OpinionQuestionOption")
  public record Option(Long id, String label) {}
}
