package com.connectlink.organization.dto;

import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;

/** 기관 한 건. */
public record OrganizationResponse(
    Long id, String code, String name, OrganizationType type, String typeLabel) {

  public static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        organization.getType(),
        organization.getType().getLabel());
  }
}
