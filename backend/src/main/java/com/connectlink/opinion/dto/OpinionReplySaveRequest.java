package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;

/**
 * 기관 회신 저장. 회신은 요청 질문에 대한 답뿐이다. 임시저장은 빈 질문이 있어도 되고, 최종제출은 필수 질문에 모두 답해야 한다.
 *
 * @param answers 질문 답
 */
public record OpinionReplySaveRequest(
    @Schema(nullable = true) @Valid List<OpinionAnswerRequest> answers) {
  public OpinionReplySaveRequest {
    answers = answers == null ? List.of() : List.copyOf(answers);
  }
}
