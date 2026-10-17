package com.connectlink.suspension;

import com.connectlink.common.Labeled;

/**
 * 일시중단 상태. 등록하면 검토 대기이고, 결제원 담당자가 한 명이라도 확인을 시작하면 검토 중, 조치·인수인계 사항을 쓰면 확정이다. 확정 건은 종료 시각이 지나면 완료가
 * 된다.
 */
public enum SuspensionStatus implements Labeled {
  PENDING_REVIEW("검토 대기"),
  IN_REVIEW("검토 중"),
  CONFIRMED("확정"),
  COMPLETED("완료");

  private final String label;

  SuspensionStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
