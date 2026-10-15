package com.connectlink.organization.dto;

import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;

/** 기관 목록 검색 조건. 비어 있는 조건은 거르지 않는다. */
public record OrganizationSearchCondition(
    String keyword, OrganizationType type, Business business, Boolean active) {}
