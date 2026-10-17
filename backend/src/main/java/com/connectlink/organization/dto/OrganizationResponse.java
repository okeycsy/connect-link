package com.connectlink.organization.dto;

import com.connectlink.organization.Business;
import com.connectlink.organization.FinanceSector;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 기관 한 건. {@code sector} 는 참가기관의 업권이다. {@code businesses} 는 기관이 참여하는 업무(업무 순서)로, 가입 화면이 고를 수 있는 업무를
 * 거른다.
 */
public record OrganizationResponse(
    Long id,
    String code,
    String name,
    OrganizationType type,
    String typeLabel,
    @Schema(nullable = true) FinanceSector sector,
    @Schema(nullable = true) String sectorLabel,
    List<Business> businesses) {

  public static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        organization.getType(),
        organization.getType().getLabel(),
        organization.getSector(),
        organization.getSector() == null ? null : organization.getSector().getLabel(),
        List.copyOf(organization.getBusinesses()));
  }
}
