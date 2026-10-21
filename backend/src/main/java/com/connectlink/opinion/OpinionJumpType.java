package com.connectlink.opinion;

import com.connectlink.common.Labeled;

/** 섹션 이동의 종류. 구글 폼의 "답변을 기준으로 섹션 이동"과 같다. 정하지 않으면 바로 다음 섹션이다. */
public enum OpinionJumpType implements Labeled {
  /** 정한 섹션으로 건너뛴다. 지금 섹션보다 뒤의 섹션만 고른다. */
  SECTION("섹션으로 이동"),
  /** 회신을 끝낸다. 뒤의 섹션은 답하지 않는다. */
  END("회신 끝내기");

  private final String label;

  OpinionJumpType(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
