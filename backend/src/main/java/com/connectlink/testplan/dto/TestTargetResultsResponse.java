package com.connectlink.testplan.dto;

import java.util.List;

/**
 * 대상 기관 한 곳의 요구사항별 결과와 결제원 의견.
 *
 * @param summary 그 기관의 수행률·결과별 건수
 * @param rows 요구사항 순서
 * @param canReport 지금 사용자가 이 기관 결과를 등록할 수 있는지(그 기관 담당자)
 * @param canFeedback 지금 사용자가 결제원 의견을 남길 수 있는지
 * @param canComment 지금 사용자가 결과가 있는 요구사항에 답글을 달 수 있는지(그 업무 결제원 담당자와 그 기관 담당자)
 */
public record TestTargetResultsResponse(
    Long planId,
    TestTargetSummaryResponse summary,
    List<TestItemResultResponse> rows,
    boolean canReport,
    boolean canFeedback,
    boolean canComment) {}
