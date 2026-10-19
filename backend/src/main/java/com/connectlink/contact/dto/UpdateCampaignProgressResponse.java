package com.connectlink.contact.dto;

import java.util.List;

/** 정기 변경 주간의 진행률과 기관별 현행화. 기관은 거르기 조건에 맞는 것만, 미완료 → 완료, 이름순이다. */
public record UpdateCampaignProgressResponse(
    UpdateCampaignResponse campaign, List<UpdateCampaignOrganizationResponse> organizations) {}
