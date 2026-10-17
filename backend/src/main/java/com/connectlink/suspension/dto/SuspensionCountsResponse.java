package com.connectlink.suspension.dto;

/** 시간 구분별 건수. 목록의 필터 칩이 쓴다. {@code pendingReview} 는 아직 확정되지 않은(검토 대기·검토 중) 진행 중·예정 건이다. */
public record SuspensionCountsResponse(
    long current, long upcoming, long past, long pendingReview) {}
