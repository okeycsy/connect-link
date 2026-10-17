package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 질문 하나. 객관식(SINGLE_CHOICE·MULTIPLE_CHOICE)은 선택지가 둘 이상 있어야 하고, 주관식(TEXT)의 선택지는 무시한다.
 *
 * @param required 최종제출 때 꼭 답해야 하는지
 * @param options 선택지 글(정한 순서)
 */
public record OpinionQuestionRequest(
    @NotNull(message = "질문 유형을 골라 주세요.") OpinionQuestionType type,
    @NotBlank(message = "질문을 입력해 주세요.") @Size(max = 500, message = "질문은 500자까지입니다.") String title,
    boolean required,
    @Schema(nullable = true) @Size(max = 20, message = "선택지는 20개까지입니다.")
        List<
                @NotBlank(message = "선택지를 입력해 주세요.") @Size(max = 200, message = "선택지는 200자까지입니다.")
                String>
            options) {
  public OpinionQuestionRequest {
    options = options == null ? List.of() : List.copyOf(options);
  }
}
