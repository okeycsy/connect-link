package com.connectlink.home.dto;

import com.connectlink.organization.Business;
import java.util.List;

/** 홈 화면 한 벌. 업무 영역(레일)마다 다르다. */
public record HomeResponse(
    Business business,
    String businessLabel,
    String userName,
    List<HomeCard.HomeCardKpi> kpis,
    List<HomeCard> cards) {}
