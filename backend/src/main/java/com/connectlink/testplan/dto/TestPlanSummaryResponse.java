package com.connectlink.testplan.dto;

import com.connectlink.organization.Business;
import com.connectlink.testplan.TestPlanState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 기관 테스트 목록 한 줄. 결제원 담당자에게는 전체 수행률과 대상 기관 수를, 기관 담당자에게는 자기 기관 수행률과 완료 요구사항 수를 준다(다른 기관 숫자는 주지 않는다).
 *
 * @param overdue 수행 기간이 지났는데 아직 완료가 아닌지
 * @param progressRate 결제원 담당자는 전체, 기관 담당자는 자기 기관 수행률(0~100)
 * @param targetCount 대상 기관 수(결제원 담당자만)
 * @param completedTargetCount 모든 요구사항을 완료한 대상 기관 수(결제원 담당자만)
 * @param ownCompletedCount 자기 기관이 완료한 요구사항 수(기관 담당자만)
 * @param ownLastReportedAt 자기 기관이 마지막으로 결과를 등록한 일시(기관 담당자만)
 */
public record TestPlanSummaryResponse(
    Long id,
    Business business,
    String businessLabel,
    String subBusiness,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    TestPlanState state,
    String stateLabel,
    boolean overdue,
    int progressRate,
    int itemCount,
    @Schema(nullable = true) Integer targetCount,
    @Schema(nullable = true) Integer completedTargetCount,
    @Schema(nullable = true) Integer ownCompletedCount,
    @Schema(nullable = true) LocalDateTime ownLastReportedAt,
    String ownerName) {}
