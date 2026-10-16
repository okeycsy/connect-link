package com.connectlink.account.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;

/** 신청한 업무 한 건과 세부업무(자유 텍스트, 없으면 null). */
public record SignUpApplicationBusinessResponse(
    Business code,
    String slug,
    String label,
    String shortLabel,
    @Schema(nullable = true) String subBusiness) {}
