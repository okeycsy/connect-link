package com.connectlink.account.dto;

/** 한 업무 영역의 가입 신청 건수. 승인·반려는 가입 승인을 거친 계정만 센다. */
public record SignUpApplicationCountsResponse(long pending, long approved, long rejected) {}
