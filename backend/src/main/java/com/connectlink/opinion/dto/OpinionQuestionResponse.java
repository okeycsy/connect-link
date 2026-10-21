package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 질문 하나.
 *
 * @param sectionIndex 묶인 섹션의 순서(0부터)
 * @param options 선택지(정한 순서). 주관식이면 비어 있다
 */
public record OpinionQuestionResponse(
    Long id,
    int sectionIndex,
    OpinionQuestionType type,
    String typeLabel,
    String title,
    boolean required,
    List<Option> options) {

  /**
   * @param jump 이 선택지를 고르면 갈 섹션. 없으면 섹션의 이동을 따른다
   */
  @Schema(name = "OpinionQuestionOption")
  public record Option(Long id, String label, @Schema(nullable = true) OpinionJump jump) {}
}
