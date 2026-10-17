package com.connectlink.organization;

import com.connectlink.common.Labeled;

/** 업권. 참가기관이 속한 금융 업종이다. 의견 요청에서 대상 기관을 업권별로 한꺼번에 고를 때 쓴다. */
public enum FinanceSector implements Labeled {
  BANK("은행"),
  SAVINGS_BANK("저축은행"),
  SECURITIES("증권"),
  CARD("카드"),
  INSURANCE("보험"),
  CAPITAL("캐피탈"),
  MUTUAL_FINANCE("상호금융"),
  ETC("기타");

  private final String label;

  FinanceSector(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
