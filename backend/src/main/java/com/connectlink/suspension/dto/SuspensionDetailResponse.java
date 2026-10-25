package com.connectlink.suspension.dto;

import com.connectlink.suspension.SuspensionKind;
import com.connectlink.suspension.SuspensionPeriod;
import com.connectlink.suspension.SuspensionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 일시중단 상세. 등록 양식 그대로와 검토·확정 기록, 지금 할 수 있는 일.
 *
 * @param targetWorks 고른 대상 업무. 기준정보 순서
 * @param businesses 관련 레일 업무. 업무 순서이고, 비면 결제원 운영·상황실·대외계가 맡는다
 * @param contacts 기관 작업 담당자 연락처. 적은 순서
 * @param handover 결제원의 조치 및 인수인계 사항. 확정하면 생긴다
 * @param registrantJobTitle 등록한 사람의 그때 직급
 * @param confirmedByJobTitle 확정한 결제원 담당자의 그때 직급
 * @param reviewers 확인을 시작한 결제원 담당자. 시작한 순서
 */
public record SuspensionDetailResponse(
    Long id,
    SuspensionKind kind,
    String kindLabel,
    List<SuspensionTargetWorkResponse> targetWorks,
    List<SuspensionBusinessResponse> businesses,
    Long organizationId,
    String organizationCode,
    String organizationName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    SuspensionPeriod period,
    String periodLabel,
    SuspensionStatus status,
    String statusLabel,
    String reason,
    @Schema(nullable = true) String kftcAction,
    List<SuspensionContactResponse> contacts,
    @Schema(nullable = true) String extraNote,
    String registrantName,
    @Schema(nullable = true) String registrantJobTitle,
    boolean registrantKftc,
    @Schema(nullable = true) String handover,
    @Schema(nullable = true) LocalDateTime confirmedAt,
    @Schema(nullable = true) String confirmedByName,
    @Schema(nullable = true) String confirmedByJobTitle,
    @Schema(nullable = true) LocalDateTime completedAt,
    List<SuspensionReviewerResponse> reviewers,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    SuspensionPermissions permissions) {}
