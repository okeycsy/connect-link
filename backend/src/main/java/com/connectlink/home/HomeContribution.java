package com.connectlink.home;

import com.connectlink.home.dto.HomeCard;

/** 기여자가 돌려주는 카드 한 장. API 로 나가는 DTO 가 아니다. */
public record HomeContribution(HomeCard card) {

  public static HomeContribution of(HomeCard card) {
    return new HomeContribution(card);
  }
}
