package com.connectlink.contact;

import com.connectlink.common.Labeled;

/** 주/부담당. 기관 하나의 업무 하나에 주담당과 부담당이 여럿 있을 수 있다(현업·전산 각각). */
public enum ContactRank implements Labeled {
  PRIMARY("주담당"),
  SECONDARY("부담당");

  private final String label;

  ContactRank(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
