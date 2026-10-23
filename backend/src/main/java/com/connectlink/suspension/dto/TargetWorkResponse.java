package com.connectlink.suspension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 일시중단 대상 업무 기준정보 하나.
 *
 * @param category 고르기 화면의 분류. 없으면 null
 * @param businesses 관련 레일 업무. 비면 결제원 운영·상황실·대외계가 맡는다
 */
public record TargetWorkResponse(
    Long id,
    String name,
    @Schema(nullable = true) String category,
    int sortOrder,
    boolean active,
    List<SuspensionBusinessResponse> businesses) {}
