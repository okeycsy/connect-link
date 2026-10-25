package com.connectlink.suspension;

import com.connectlink.common.Labeled;

/**
 * 일시중단 종류. 참가기관이 업무를 멈추는 기관 일시중단과, 결제원이 시스템 작업으로 업무를 멈추는 결제원 작업 안내다. 결제원 작업은 결제원 담당자가 올리면 바로
 * 확정이고(따로 확인하지 않는다) 관련 업무의 참가기관 담당자가 보고 메일을 받는다.
 */
public enum SuspensionKind implements Labeled {
  ORGANIZATION("기관 일시중단"),
  KFTC_WORK("결제원 작업");

  private final String label;

  SuspensionKind(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
