package com.connectlink.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 리마인드 메일 보내기.
 *
 * @param organizationIds 보낼 기관. 비우면 아직 "현행화 완료" 를 누르지 않은 대상 기관 전부다. 이미 완료했거나 대상이 아닌 기관은 건너뛴다
 */
public record UpdateCampaignReminderRequest(@Schema(nullable = true) List<Long> organizationIds) {}
