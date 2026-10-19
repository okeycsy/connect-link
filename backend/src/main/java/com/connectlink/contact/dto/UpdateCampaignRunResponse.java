package com.connectlink.contact.dto;

/**
 * 정기 변경 예약 작업을 한 번 돌린 결과.
 *
 * @param createdCampaignCount 주기가 돌아와 새로 등록한 주간 수
 * @param remindedOrganizationCount 리마인드 메일을 보낸 기관 수
 */
public record UpdateCampaignRunResponse(int createdCampaignCount, int remindedOrganizationCount) {}
