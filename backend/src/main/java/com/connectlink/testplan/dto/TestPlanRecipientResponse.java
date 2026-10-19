package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 대상 후보 한 명(그 업무를 맡은 사용 중 결제원 계정). 이미 골랐는데 지금은 후보가 아닌 사람(업무를 내려놓았거나 탈퇴)은 {@code accountId} 가
 * null 일 수 있고 {@code candidate} 가 거짓이다.
 *
 * @param jobRoleLabel 직무 표기(예: "전산(개발)")
 * @param selected 지금 알림을 받는지
 * @param candidate 지금 후보인지
 */
public record TestPlanRecipientResponse(
    @Schema(nullable = true) Long accountId,
    String name,
    String email,
    @Schema(nullable = true) String jobRoleLabel,
    @Schema(nullable = true) String department,
    @Schema(nullable = true) String team,
    boolean selected,
    boolean candidate) {}
