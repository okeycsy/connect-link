package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 업권별 회신 양식 한 개. 양식 파일은 저장한 뒤 첨부(OPINION_REPLY_FORM, 양식 ID)로 올린다.
 *
 * @param id 이미 있는 양식이면 그 ID. 새 양식이면 null
 * @param name 양식 이름(예: 은행권). 요청 안에서 겹치지 않는다. 대상 기관이 이 이름으로 양식을 고른다
 */
public record OpinionReplyFormRequest(
    @Schema(nullable = true) Long id,
    @NotBlank(message = "양식 이름을 입력해 주세요.") @Size(max = 50, message = "양식 이름은 50자까지입니다.")
        String name,
    @Schema(nullable = true) @Size(max = 500, message = "양식 안내는 500자까지입니다.") String description) {}
