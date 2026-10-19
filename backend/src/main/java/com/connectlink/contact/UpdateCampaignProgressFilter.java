package com.connectlink.contact;

import com.connectlink.common.Labeled;

/** 정기 변경 진행률의 기관 목록 거르기. 명세 "미확인 기관 및 정보 변경 기관 별도 조회". */
public enum UpdateCampaignProgressFilter implements Labeled {
  ALL("전체"),
  COMPLETED("현행화 완료"),
  INCOMPLETE("미확인"),
  CHANGED("정보 변경");

  private final String label;

  UpdateCampaignProgressFilter(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
