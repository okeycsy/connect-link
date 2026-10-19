package com.connectlink.history;

import com.connectlink.common.Labeled;

/** 변경 이력의 대상 종류. 기능 모듈이 자기 대상을 더하면서 {@link HistoryTargetAccess} 빈을 함께 만든다. */
public enum HistoryTargetType implements Labeled {
  ORGANIZATION("기관"),
  CONTACT("담당자"),
  OPINION_REQUEST("의견 요청"),
  OPINION_REPLY("기관 회신"),
  TEST_PLAN("기관 테스트"),
  TEST_PLAN_TARGET("기관 테스트 결과"),
  SUSPENSION("일시중단");

  private final String label;

  HistoryTargetType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
