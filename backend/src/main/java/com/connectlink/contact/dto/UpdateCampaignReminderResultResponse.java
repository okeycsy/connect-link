package com.connectlink.contact.dto;

/**
 * 리마인드 메일을 보낸 결과.
 *
 * @param organizationCount 리마인드를 보낸 기관 수
 * @param recipientCount 메일을 받은 사람 수
 * @param skippedOrganizationCount 받을 사람(계정·담당자 이메일)이 없어 보내지 못한 기관 수
 */
public record UpdateCampaignReminderResultResponse(
    int organizationCount, int recipientCount, int skippedOrganizationCount) {}
