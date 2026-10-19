package com.connectlink.testplan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * 기관 결과에 대한 결제원 의견. 둘 다 비우면 의견을 지운다.
 *
 * @param rate 미니 수행률(0~100). 100 이면 결제원도 수행완료로 본다
 * @param note 특이사항(예: 003 전문에 대해 예외사항 발생)
 */
public record TestFeedbackRequest(
    @Schema(nullable = true)
        @Min(value = 0, message = "미니 수행률은 0 이상입니다.")
        @Max(value = 100, message = "미니 수행률은 100 이하입니다.")
        Integer rate,
    @Schema(nullable = true) @Size(max = 2000, message = "특이사항은 2000자까지입니다.") String note) {}
