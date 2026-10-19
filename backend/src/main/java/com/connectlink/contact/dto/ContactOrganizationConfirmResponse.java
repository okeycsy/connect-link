package com.connectlink.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 기관 현행화 확인 결과.
 *
 * @param confirmedCount 최근 확인일을 갱신한 담당자 수
 * @param campaignTitle 진행 중인 정기 변경 주간에 "현행화 완료" 로 남겼으면 그 주간 이름. 진행 중인 주간이 없으면 null
 */
public record ContactOrganizationConfirmResponse(
    int confirmedCount, @Schema(nullable = true) String campaignTitle) {}
