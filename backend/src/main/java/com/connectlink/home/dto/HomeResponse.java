package com.connectlink.home.dto;

import com.connectlink.organization.Business;
import java.util.List;

/** 홈 화면 한 벌. 업무 영역(레일)마다, 사람마다 다르다. 이 사람에게 할 일이 있는 카드만 든다. */
public record HomeResponse(
    Business business, String businessLabel, String userName, List<HomeCard> cards) {}
