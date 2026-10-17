package com.connectlink.opinion.dto;

import com.connectlink.opinion.OpinionReplyType;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Size;

/**
 * 기관 회신 저장. 임시저장은 빈 칸이 있어도 되고, 최종제출은 의견 유형이 꼭 있어야 하며 조건부 동의·이견은 세부의견도 있어야 한다.
 *
 * @param type 의견 유형
 * @param content 세부의견
 */
public record OpinionReplySaveRequest(
    @Schema(nullable = true) OpinionReplyType type,
    @Schema(nullable = true) @Size(max = 4000, message = "세부의견은 4000자까지입니다.") String content) {}
