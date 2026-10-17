package com.connectlink.opinion;

import com.connectlink.common.Labeled;

/** 기관 회신 상태. 제출한 뒤 고치려면 수정 요청을 하고 결제원 담당자가 승인해야 한다. */
public enum OpinionReplyStatus implements Labeled {
  DRAFT("임시저장"),
  SUBMITTED("제출"),
  REVISION_REQUESTED("수정 요청"),
  REVISING("수정 중");

  private final String label;

  OpinionReplyStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
