package com.connectlink.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 정기 변경 주간의 기관 하나.
 *
 * @param completed "현행화 완료" 를 눌렀는지
 * @param changed 주간 동안 담당자 정보가 등록·수정·삭제되었는지
 * @param contactCount 지금 등록된 그 업무 담당자 수
 */
public record UpdateCampaignOrganizationResponse(
    Long organizationId,
    String organizationCode,
    String organizationName,
    boolean completed,
    @Schema(nullable = true) LocalDateTime completedAt,
    @Schema(nullable = true) String completedByName,
    boolean changed,
    @Schema(nullable = true) LocalDateTime changedAt,
    long contactCount) {}
