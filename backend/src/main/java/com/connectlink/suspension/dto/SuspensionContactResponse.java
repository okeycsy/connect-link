package com.connectlink.suspension.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 일시중단 양식의 기관 작업 담당자 한 명. */
public record SuspensionContactResponse(
    String name, String phone, @Schema(nullable = true) String email) {}
