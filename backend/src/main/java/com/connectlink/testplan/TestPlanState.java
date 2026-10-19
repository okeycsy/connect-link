package com.connectlink.testplan;

import com.connectlink.common.Labeled;

/** 기관 테스트의 진행 구분. 저장하지 않고 수행 완료 일시로 정한다: 모든 대상 기관의 모든 요구사항이 완료면 수행 완료, 아니면 진행 중이다. */
public enum TestPlanState implements Labeled {
  IN_PROGRESS("진행 중"),
  COMPLETED("수행 완료");

  private final String label;

  TestPlanState(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
