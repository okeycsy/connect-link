package com.connectlink.home;

import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardKpi;

/** 기여자가 돌려주는 한 묶음: 카드와, 있으면 밴드 KPI. API 로 나가는 DTO 가 아니다. */
public record HomeContribution(HomeCard card, HomeCardKpi kpi) {

  public static HomeContribution of(HomeCard card) {
    return new HomeContribution(card, null);
  }
}
