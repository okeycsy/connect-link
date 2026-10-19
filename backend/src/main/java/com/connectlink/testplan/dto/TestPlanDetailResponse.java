package com.connectlink.testplan.dto;

import com.connectlink.organization.Business;
import com.connectlink.testplan.TestPlanState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기관 테스트 상세. 기관 담당자에게는 {@code targets} 에 자기 기관 하나만, {@code progressRate} 에 자기 기관 수행률을 준다.
 *
 * @param overdue 수행 기간이 지났는데 아직 완료가 아닌지
 * @param progressRate 결제원 담당자는 전체, 기관 담당자는 자기 기관 수행률(0~100)
 * @param completedAt 수행 완료 일시(모든 대상 기관의 모든 요구사항이 완료된 때)
 * @param ownOrganizationId 기관 담당자의 자기 기관 ID. 결제원 담당자면 null
 * @param recipients 결과 등록 매터모스트 알림을 받는 결제원 담당자 이름(결제원 담당자만, 아니면 비어 있다)
 */
public record TestPlanDetailResponse(
    Long id,
    Business business,
    String businessSlug,
    String businessLabel,
    String subBusiness,
    String title,
    @Schema(nullable = true) String description,
    LocalDate startDate,
    LocalDate endDate,
    TestPlanState state,
    String stateLabel,
    boolean overdue,
    int progressRate,
    @Schema(nullable = true) LocalDateTime completedAt,
    String ownerName,
    List<TestItemResponse> items,
    List<TestTargetSummaryResponse> targets,
    @Schema(nullable = true) Long ownOrganizationId,
    List<String> recipients,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    TestPlanPermissions permissions) {}
