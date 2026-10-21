package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.Size;

/**
 * 섹션 하나. 질문은 {@link OpinionQuestionRequest#sectionIndex()} 로 이 섹션에 묶는다.
 *
 * @param title 섹션 제목. 섹션이 하나뿐이면 비워도 된다
 * @param jump 이 섹션 다음 이동. 없으면 바로 다음 섹션
 */
public record OpinionSectionRequest(
    @Schema(nullable = true) @Size(max = 200, message = "섹션 제목은 200자까지입니다.") String title,
    @Schema(nullable = true) @Size(max = 2000, message = "섹션 설명은 2000자까지입니다.") String description,
    @Schema(nullable = true) @Valid OpinionJump jump) {}
