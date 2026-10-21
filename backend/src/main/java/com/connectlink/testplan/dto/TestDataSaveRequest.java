package com.connectlink.testplan.dto;

import com.connectlink.testplan.TestDataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 기관 담당자가 자기 기관의 테스트데이터 준비 상태를 저장한다. 파일은 첨부 API(ownerType TEST_DATA, ownerId = targetId)로 따로 올린다.
 *
 * @param note 만든 테스트데이터 설명이나 못 만든 이유
 */
public record TestDataSaveRequest(
    @NotNull(message = "생성 여부를 골라 주세요.") TestDataStatus status,
    @Schema(nullable = true) @Size(max = 2000, message = "메모는 2000자까지입니다.") String note) {}
