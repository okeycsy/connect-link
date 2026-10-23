package com.connectlink.suspension.dto;

import com.connectlink.suspension.SuspensionPeriod;
import com.connectlink.suspension.SuspensionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 일시중단 목록 한 줄. 등록한 사람은 그때의 이름·직급으로 "결제원(김민찬 과장)"처럼 보인다. */
public record SuspensionSummaryResponse(
    Long id,
    List<SuspensionTargetWorkResponse> targetWorks,
    List<SuspensionBusinessResponse> businesses,
    Long organizationId,
    String organizationName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    SuspensionPeriod period,
    String periodLabel,
    SuspensionStatus status,
    String statusLabel,
    String reason,
    String registrantName,
    @Schema(nullable = true) String registrantJobTitle,
    boolean registrantKftc,
    int reviewerCount) {}
