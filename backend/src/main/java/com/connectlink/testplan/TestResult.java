package com.connectlink.testplan;

import com.connectlink.common.Labeled;

/** 참가기관이 요구사항 하나에 등록하는 테스트 결과. 오류면 특이사항(오류내용)을 함께 쓴다. 기관 쪽 수행완료는 정상이다. */
public enum TestResult implements Labeled {
  PASSED("정상"),
  FAILED("오류"),
  IN_PROGRESS("수행중"),
  NOT_STARTED("미수행");

  private final String label;

  TestResult(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
