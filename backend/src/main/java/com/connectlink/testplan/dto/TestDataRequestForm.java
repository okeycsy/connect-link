package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 기관 테스트 만들기·고치기의 테스트데이터 사전 요청. 테스트데이터는 수행 기간 전에 받으므로 회신 기한은 수행 시작일과 같거나 앞이어야 한다. 회신 양식은 저장한 뒤 첨부
 * API(TEST_DATA_FORM)로 올린다.
 *
 * @param dueDate 테스트데이터 회신 기한
 * @param instruction 요청 사항(어떤 데이터를 몇 건 만들지 등)
 */
public record TestDataRequestForm(
    @NotNull(message = "테스트데이터 회신 기한을 골라 주세요.") LocalDate dueDate,
    @Schema(nullable = true) @Size(max = 2000, message = "요청 사항은 2000자까지입니다.")
        String instruction) {}
