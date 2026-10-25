package com.connectlink.notice;

import com.connectlink.common.Labeled;

/** 공지·자료실 글의 분류. 공지는 업무별이거나 전 업무 공통이고, 규약·시행세칙은 업무별 자료다. */
public enum NoticeCategory implements Labeled {
  NOTICE("공지"),
  RULE("규약"),
  ENFORCEMENT_RULE("시행세칙");

  private final String label;

  NoticeCategory(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }

  /** 업무 자료(규약·시행세칙)인지. 자료는 전 업무 공통으로 둘 수 없다. */
  public boolean isRegulation() {
    return this != NOTICE;
  }
}
