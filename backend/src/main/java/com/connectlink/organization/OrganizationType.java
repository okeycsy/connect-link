package com.connectlink.organization;

/** 기관 구분. */
public enum OrganizationType {
  /** 결제원(KFTC). */
  KFTC("결제원"),
  /** 참가기관. 은행, 증권사 등 금융회사. */
  FINANCE_ORGANIZATION("참가기관"),
  /** 이용기관. 금융회사가 아닌 일반 회사(주로 결제원의 고객). */
  USE_ORGANIZATION("이용기관");

  private final String label;

  OrganizationType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
