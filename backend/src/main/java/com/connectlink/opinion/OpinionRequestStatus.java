package com.connectlink.opinion;

import com.connectlink.common.Labeled;

/** 의견 요청 상태. 작성중에는 대상 기관에 보이지 않고, 회신중에만 기관이 회신하며, 마감하면 더 받지 않는다. */
public enum OpinionRequestStatus implements Labeled {
  DRAFT("작성중"),
  OPEN("회신중"),
  CLOSED("마감");

  private final String label;

  OpinionRequestStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
