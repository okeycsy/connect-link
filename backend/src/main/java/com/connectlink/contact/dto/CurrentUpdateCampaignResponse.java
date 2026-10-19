package com.connectlink.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 연락처 화면 위쪽 안내에 쓰는 지금 진행 중인 정기 변경 주간.
 *
 * @param campaign 진행 중인 주간. 없으면 null
 * @param ownOrganization 기관 담당자의 자기 기관 현행화. 결제원 담당자거나 진행 중인 주간이 없으면 null
 */
public record CurrentUpdateCampaignResponse(
    @Schema(nullable = true) UpdateCampaignResponse campaign,
    @Schema(nullable = true) UpdateCampaignOrganizationResponse ownOrganization) {}
