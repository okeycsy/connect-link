package com.connectlink.testplan.dto;

import com.connectlink.testplan.TestDataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 대상 기관 한 곳의 테스트 전 테스트데이터 준비 상태.
 *
 * @param targetId 대상 기관 행 ID. 테스트데이터 파일 첨부(TEST_DATA)의 ownerId 다
 * @param fileCount 올린 테스트데이터 파일 수
 * @param reportedByName 생성 여부·메모를 마지막으로 바꾼 기관 담당자
 * @param canEdit 지금 사용자가 이 기관 테스트데이터를 고치고 파일을 올릴 수 있는지(그 기관 담당자)
 */
public record TestDataResponse(
    Long planId,
    Long targetId,
    Long organizationId,
    String organizationName,
    TestDataStatus status,
    String statusLabel,
    @Schema(nullable = true) String note,
    int fileCount,
    @Schema(nullable = true) String reportedByName,
    @Schema(nullable = true) LocalDateTime reportedAt,
    boolean canEdit) {}
