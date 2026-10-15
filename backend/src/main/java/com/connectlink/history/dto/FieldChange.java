package com.connectlink.history.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 항목 하나의 변경 전·후 값. 값은 화면에 그대로 보여 주는 문자열이다(날짜·목록도 기록하는 쪽이 문자열로 만든다). 등록이면 before, 삭제면 after 가 비어 있다.
 */
public record FieldChange(
    String field,
    String label,
    @Schema(nullable = true) String before,
    @Schema(nullable = true) String after) {}
