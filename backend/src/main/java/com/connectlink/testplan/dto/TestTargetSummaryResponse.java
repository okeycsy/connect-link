package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 대상 기관 한 곳의 현황. 결제원 담당자는 전 기관을, 기관 담당자는 자기 기관만 받는다.
 *
 * @param targetId 대상 기관 행 ID. 기관별 결과 변경 이력(TEST_PLAN_TARGET)의 대상이다
 * @param completedCount 완료한 요구사항 수(기관 정상 + 결제원 100%)
 * @param unreported 아직 결과를 등록하지 않은 요구사항 수
 * @param awaitingReview 결제원이 확인할 결과 수(오류, 또는 정상인데 결제원 의견이 없거나 지난 의견)
 * @param completed 모든 요구사항을 완료했는지
 */
public record TestTargetSummaryResponse(
    Long targetId,
    Long organizationId,
    String organizationCode,
    String organizationName,
    int progressRate,
    int completedCount,
    int itemCount,
    int passed,
    int failed,
    int inProgress,
    int notStarted,
    int unreported,
    int awaitingReview,
    boolean completed,
    @Schema(nullable = true) LocalDateTime lastReportedAt) {}
