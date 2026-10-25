package com.connectlink.organization.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 기관 한눈에 보기: 한 기관의 그 업무 담당자·의견 요청·기관 테스트·일시중단. 볼 수 있는 칸만 들어 있다. */
public record OrganizationOverviewResponse(
    Long organizationId,
    String organizationCode,
    String organizationName,
    @Schema(nullable = true) String sectorLabel,
    Business business,
    List<OrganizationOverviewSection> sections) {}
