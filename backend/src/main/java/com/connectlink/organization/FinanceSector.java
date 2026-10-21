package com.connectlink.organization;

import com.connectlink.common.Labeled;

/**
 * 업권. 참가기관이 속한 금융 업종이다(우체국·상호금융·저축은행 등은 서민금융기관). 참가기관은 반드시 하나를 가지고 결제원·이용기관은 비워 둔다. 의견 요청에서 대상
 * 기관을 업권별로 한꺼번에 고를 때 쓴다.
 */
public enum FinanceSector implements Labeled {
  BANK("은행"),
  SECURITIES("증권사"),
  MICROFINANCE("서민금융기관"),
  INSURANCE("보험사");

  private final String label;

  FinanceSector(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
