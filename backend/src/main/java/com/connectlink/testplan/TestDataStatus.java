package com.connectlink.testplan;

import com.connectlink.common.Labeled;

/** 대상 기관의 테스트 전 테스트데이터 생성 여부. 기관 담당자가 정한다. */
public enum TestDataStatus implements Labeled {
  NOT_CREATED("미생성"),
  CREATED("생성 완료");

  private final String label;

  TestDataStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
