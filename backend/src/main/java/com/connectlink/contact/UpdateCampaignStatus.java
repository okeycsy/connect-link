package com.connectlink.contact;

import com.connectlink.common.Labeled;
import java.time.LocalDate;

/** 정기 변경 주간의 진행 구분. 저장하지 않고 오늘 날짜와 기간으로 정한다. */
public enum UpdateCampaignStatus implements Labeled {
  UPCOMING("예정"),
  OPEN("진행 중"),
  CLOSED("종료");

  private final String label;

  UpdateCampaignStatus(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }

  static UpdateCampaignStatus of(LocalDate startDate, LocalDate endDate, LocalDate today) {
    if (today.isBefore(startDate)) {
      return UPCOMING;
    }
    return today.isAfter(endDate) ? CLOSED : OPEN;
  }
}
