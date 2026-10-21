package com.connectlink.opinion.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 의견 요청 만들기. 작성중으로 저장되고, 첨부자료는 저장한 뒤 첨부 API 로 올린다. 섹션을 비우면 제목 없는 섹션 하나다. */
public record OpinionRequestCreateRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @NotBlank(message = "요청명을 입력해 주세요.") @Size(max = 200, message = "요청명은 200자까지입니다.") String title,
    @NotBlank(message = "요청 목적·내용을 입력해 주세요.") @Size(max = 4000, message = "요청 내용은 4000자까지입니다.")
        String content,
    @NotNull(message = "회신 기한을 골라 주세요.") LocalDate dueDate,
    @Schema(nullable = true) @Valid @Size(max = 30, message = "섹션은 30개까지입니다.")
        List<OpinionSectionRequest> sections,
    @Valid @Size(max = 30, message = "질문은 30개까지입니다.") List<OpinionQuestionRequest> questions,
    @Valid List<OpinionTargetRequest> targets) {

  public OpinionRequestCreateRequest {
    sections = sections == null ? List.of() : List.copyOf(sections);
    questions = questions == null ? List.of() : List.copyOf(questions);
    targets = targets == null ? List.of() : List.copyOf(targets);
  }
}
