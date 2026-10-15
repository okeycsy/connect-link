package com.connectlink.organization.dto;

import com.connectlink.organization.Business;

/** 업무 영역 한 건. */
public record BusinessResponse(Business code, String slug, String label, String shortLabel) {

  public static BusinessResponse from(Business business) {
    return new BusinessResponse(
        business, business.getSlug(), business.getLabel(), business.getShortLabel());
  }
}
