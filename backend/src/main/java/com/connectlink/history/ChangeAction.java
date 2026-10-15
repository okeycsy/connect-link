package com.connectlink.history;

import com.connectlink.common.Labeled;

/** 변경 이력 한 건이 어떤 일인지. */
public enum ChangeAction implements Labeled {
  CREATED("등록"),
  UPDATED("수정"),
  DELETED("삭제");

  private final String label;

  ChangeAction(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
