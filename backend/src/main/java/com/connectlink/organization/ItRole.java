package com.connectlink.organization;

import com.connectlink.common.Labeled;

/** 전산 세부 직무. 여러 개 고를 수 있다. 결제원의 운영·상황실·대외계는 전 업무를 조회하는 근거가 된다. */
public enum ItRole implements Labeled {
  DEV("개발"),
  OPS("운영"),
  INFRA("인프라"),
  EXTERNAL_LINK("대외계"),
  CONTROL_ROOM("상황실");

  private final String label;

  ItRole(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
