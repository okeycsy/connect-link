package com.connectlink.suspension;

import com.connectlink.common.Labeled;

/** 목록의 시간 구분. 상태와 따로 시작·종료 시각과 지금을 견주어 정한다. */
public enum SuspensionPeriod implements Labeled {
  /** 지금 중단 중(시작 ≤ 지금 < 종료). */
  CURRENT("진행 중"),
  /** 앞으로 중단할 예정(지금 < 시작). */
  UPCOMING("예정"),
  /** 끝난 중단(종료 ≤ 지금). 기관별 과거 이력이다. */
  PAST("지난");

  private final String label;

  SuspensionPeriod(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
