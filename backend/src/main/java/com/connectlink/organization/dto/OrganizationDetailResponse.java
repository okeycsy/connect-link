package com.connectlink.organization.dto;

import com.connectlink.organization.FinanceSector;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/** 기관 기준정보 한 건(관리 화면용). 공개 목록의 {@link OrganizationResponse} 와 달리 참여 업무, 이메일 도메인, 감사 정보를 담는다. */
public record OrganizationDetailResponse(
    Long id,
    String code,
    String name,
    OrganizationType type,
    String typeLabel,
    @Schema(nullable = true) String emailDomain,
    boolean active,
    @Schema(nullable = true) FinanceSector sector,
    @Schema(nullable = true) String sectorLabel,
    List<BusinessResponse> businesses,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy) {

  public static OrganizationDetailResponse from(Organization organization) {
    return new OrganizationDetailResponse(
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        organization.getType(),
        organization.getType().getLabel(),
        organization.getEmailDomain(),
        organization.isActive(),
        organization.getSector(),
        organization.getSector() == null ? null : organization.getSector().getLabel(),
        organization.getBusinesses().stream().map(BusinessResponse::from).toList(),
        organization.getCreatedAt(),
        organization.getCreatedBy(),
        organization.getUpdatedAt(),
        organization.getUpdatedBy());
  }
}
