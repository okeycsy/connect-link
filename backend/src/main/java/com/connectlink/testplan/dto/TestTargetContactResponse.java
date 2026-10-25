package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 대상 기관의 테스트 담당자 한 명(고를 때의 값).
 *
 * @param subBusiness 고를 때의 세부업무. 없으면 null
 */
public record TestTargetContactResponse(
    String name, String email, @Schema(nullable = true) String subBusiness) {}
