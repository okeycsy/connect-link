package com.connectlink.testplan.dto;

import com.connectlink.testplan.TestResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 기관 담당자가 자기 기관 결과를 한 번에 저장한다. 고른 요구사항만 보내도 되고, 바뀌지 않은 행은 그대로 둔다. */
public record TestResultSaveRequest(
    @Valid @NotNull(message = "저장할 결과를 보내 주세요.") List<Entry> results) {

  public TestResultSaveRequest {
    results = results == null ? List.of() : List.copyOf(results);
  }

  /**
   * 요구사항 하나의 결과.
   *
   * @param note 특이사항. 오류면 오류내용을 꼭 쓴다
   */
  public record Entry(
      @NotNull(message = "요구사항을 골라 주세요.") Long itemId,
      @NotNull(message = "결과를 골라 주세요.") TestResult result,
      @Schema(nullable = true) @Size(max = 2000, message = "특이사항은 2000자까지입니다.") String note) {}
}
