package com.connectlink.testplan.dto;

import java.time.LocalDateTime;

/**
 * 테스트데이터 요청 메일을 다시 보낸 결과.
 *
 * @param organizationCount 요청을 보낸 기관 수(아직 생성 완료가 아닌 대상 기관)
 */
public record TestDataRequestSentResponse(int organizationCount, LocalDateTime requestedAt) {}
