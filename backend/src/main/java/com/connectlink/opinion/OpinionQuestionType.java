package com.connectlink.opinion;

import com.connectlink.common.Labeled;

/** 의견 요청 질문의 유형. 구글 폼처럼 객관식(하나·여러 개 고르기)과 주관식을 둔다. */
public enum OpinionQuestionType implements Labeled {
  SINGLE_CHOICE("객관식"),
  MULTIPLE_CHOICE("체크박스"),
  TEXT("주관식");

  private final String label;

  OpinionQuestionType(String label) {
    this.label = label;
  }

  /** 선택지를 고르는 질문인지. */
  public boolean isChoice() {
    return this != TEXT;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
