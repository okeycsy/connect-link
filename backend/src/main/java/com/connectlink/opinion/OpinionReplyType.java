package com.connectlink.opinion;

import com.connectlink.common.Labeled;

/** 기관 회신의 의견 유형. 명세의 정형화된 유형이다. */
public enum OpinionReplyType implements Labeled {
  AGREE("동의"),
  CONDITIONAL("조건부 동의"),
  DISAGREE("이견");

  private final String label;

  OpinionReplyType(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
