package com.connectlink.testplan.dto;

import com.connectlink.organization.FinanceSector;
import io.swagger.v3.oas.annotations.media.Schema;

/** 대상 기관 후보 한 곳. 그 업무에 참여하는 사용 중 참가기관이다. 업권으로 묶어 고른다. */
public record TestPlanOrganizationResponse(
    Long id,
    String code,
    String name,
    @Schema(nullable = true) FinanceSector sector,
    @Schema(nullable = true) String sectorLabel) {}
