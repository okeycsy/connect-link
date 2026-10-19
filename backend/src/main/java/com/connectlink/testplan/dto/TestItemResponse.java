package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 요구사항 하나.
 *
 * @param number 화면 번호("001" 꼴, 순서대로)
 * @param description 확인 사항
 */
public record TestItemResponse(
    Long id, String number, String title, @Schema(nullable = true) String description) {}
