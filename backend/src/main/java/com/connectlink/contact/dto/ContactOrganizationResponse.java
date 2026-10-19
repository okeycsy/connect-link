package com.connectlink.contact.dto;

import com.connectlink.organization.OrganizationType;

/** 담당자를 등록하거나 목록을 거를 때 고르는 기관 한 곳. */
public record ContactOrganizationResponse(
    Long id, String code, String name, OrganizationType type) {}
