package com.connectlink.suspension.dto;

import com.connectlink.organization.Business;
import com.connectlink.suspension.SuspensionPeriod;
import com.connectlink.suspension.SuspensionStatus;
import java.time.LocalDateTime;

/** 일시중단 목록 한 줄. */
public record SuspensionSummaryResponse(
    Long id,
    Business business,
    String businessLabel,
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
    boolean registrantKftc,
    int reviewerCount) {}
