package com.connectlink.organization.dto;

import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.util.List;

/** 기관 한 건. {@code businesses} 는 기관이 참여하는 업무(업무 순서)로, 가입 화면이 고를 수 있는 업무를 거른다. */
public record OrganizationResponse(
    Long id,
    String code,
    String name,
    OrganizationType type,
    String typeLabel,
    List<Business> businesses) {

  public static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        organization.getType(),
        organization.getType().getLabel(),
        List.copyOf(organization.getBusinesses()));
  }
}
