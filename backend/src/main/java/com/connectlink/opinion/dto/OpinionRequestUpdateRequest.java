package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 의견 요청 고치기. 작성중과 회신중에 고친다. 업무는 바꾸지 않는다. 섹션·질문·대상은 보낸 목록으로 통째로 바뀐다. 섹션·질문은 작성중에만 바꿀 수 있다. 섹션을 비우면
 * 제목 없는 섹션 하나다.
 */
public record OpinionRequestUpdateRequest(
    @NotBlank(message = "요청명을 입력해 주세요.") @Size(max = 200, message = "요청명은 200자까지입니다.") String title,
    @NotBlank(message = "요청 목적·내용을 입력해 주세요.") @Size(max = 4000, message = "요청 내용은 4000자까지입니다.")
        String content,
    @NotNull(message = "회신 기한을 골라 주세요.") LocalDate dueDate,
    @Schema(nullable = true) @Valid @Size(max = 30, message = "섹션은 30개까지입니다.")
        List<OpinionSectionRequest> sections,
    @Valid @Size(max = 30, message = "질문은 30개까지입니다.") List<OpinionQuestionRequest> questions,
    @Valid List<OpinionTargetRequest> targets) {

  public OpinionRequestUpdateRequest {
    sections = sections == null ? List.of() : List.copyOf(sections);
    questions = questions == null ? List.of() : List.copyOf(questions);
    targets = targets == null ? List.of() : List.copyOf(targets);
  }
}
