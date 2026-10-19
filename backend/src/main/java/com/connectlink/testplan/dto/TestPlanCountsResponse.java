package com.connectlink.testplan.dto;

/** 진행 구분별 건수. 목록의 필터 칩이 쓴다. */
public record TestPlanCountsResponse(long inProgress, long completed) {}
