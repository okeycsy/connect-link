package com.connectlink.testplan.dto;

import com.connectlink.testplan.TestResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 대상 기관 하나의 요구사항 한 줄. 아직 결과를 등록하지 않았으면 결과 칸이 비어 있다.
 *
 * @param note 기관 특이사항(오류면 오류내용)
 * @param feedbackRate 결제원 미니 수행률(0~100)
 * @param feedbackOutdated 결제원 의견 뒤에 기관이 결과를 다시 등록해 결제원이 다시 확인해야 하는지. 그러면 그 의견은 수행률·완료에 쓰지 않는다
 * @param effectiveRate 이 요구사항의 수행률. 유효한 미니 수행률이 있으면 그 값, 없으면 정상 100 그 밖 0
 * @param completed 기관 정상 + 결제원 100% 로 수행 완료인지
 */
public record TestItemResultResponse(
    Long itemId,
    String number,
    String title,
    @Schema(nullable = true) String description,
    @Schema(nullable = true) TestResult result,
    @Schema(nullable = true) String resultLabel,
    @Schema(nullable = true) String note,
    @Schema(nullable = true) String reportedByName,
    @Schema(nullable = true) LocalDateTime reportedAt,
    @Schema(nullable = true) Integer feedbackRate,
    @Schema(nullable = true) String feedbackNote,
    @Schema(nullable = true) String feedbackByName,
    @Schema(nullable = true) LocalDateTime feedbackAt,
    boolean feedbackOutdated,
    int effectiveRate,
    boolean completed) {}
