package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 기관 테스트의 테스트데이터 사전 요청 현황.
 *
 * @param dueDate 회신 기한(수행 시작일 이전)
 * @param instruction 요청 사항
 * @param requestedAt 요청 메일을 마지막으로 보낸 일시
 * @param overdue 회신 기한이 지났는지
 * @param createdCount 생성 완료한 대상 기관 수(기관 담당자는 자기 기관만 센다)
 * @param targetCount 대상 기관 수(기관 담당자는 1)
 */
public record TestDataRequestResponse(
    LocalDate dueDate,
    @Schema(nullable = true) String instruction,
    @Schema(nullable = true) LocalDateTime requestedAt,
    boolean overdue,
    int createdCount,
    int targetCount) {}
