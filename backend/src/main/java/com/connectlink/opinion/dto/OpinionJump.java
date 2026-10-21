package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionJumpType;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;

/**
 * 섹션 이동 하나. 이동이 없으면(null) 바로 다음 섹션이다.
 *
 * @param sectionIndex 이동할 섹션의 순서(0부터, 섹션 목록 안의 위치). SECTION 일 때만 쓰고, 지금 섹션보다 뒤여야 한다
 */
public record OpinionJump(
    @NotNull(message = "이동할 곳을 골라 주세요.") OpinionJumpType type,
    @Schema(nullable = true) Integer sectionIndex) {}
