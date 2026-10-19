package com.connectlink.contact.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;

/**
 * 기관 하나의 한 업무 담당자 전부를 "변동 없음" 으로 확인한다(정기 변경 주간의 "현행화 완료"). 기관 담당자는 자기 기관으로 정해진다({@code
 * organizationId} 를 무시한다).
 */
public record ContactOrganizationConfirmRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @Schema(nullable = true) Long organizationId) {}
