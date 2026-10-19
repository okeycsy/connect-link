package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 요구사항 하나.
 *
 * @param id 고치는 기존 요구사항. 새로 넣으면 null
 * @param description 확인 사항(기대 결과, 수행 방법 등)
 */
public record TestItemRequest(
    @Schema(nullable = true) Long id,
    @NotBlank(message = "요구사항을 입력해 주세요.") @Size(max = 500, message = "요구사항은 500자까지입니다.")
        String title,
    @Schema(nullable = true) @Size(max = 2000, message = "확인 사항은 2000자까지입니다.")
        String description) {}
