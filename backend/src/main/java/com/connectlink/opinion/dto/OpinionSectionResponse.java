package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 섹션 하나. 질문은 {@link OpinionQuestionResponse#sectionIndex()} 로 이 섹션을 가리킨다.
 *
 * @param index 섹션 순서(0부터). 이동이 가리키는 값이다
 * @param jump 이 섹션 다음 이동. 없으면 바로 다음 섹션
 */
public record OpinionSectionResponse(
    Long id,
    int index,
    @Schema(nullable = true) String title,
    @Schema(nullable = true) String description,
    @Schema(nullable = true) OpinionJump jump) {}
