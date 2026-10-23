package com.connectlink.suspension.dto;

import com.connectlink.organization.Business;

/** 일시중단이 멈추는 업무 하나. */
public record SuspensionBusinessResponse(Business code, String slug, String label) {

  public static SuspensionBusinessResponse of(Business business) {
    return new SuspensionBusinessResponse(business, business.getSlug(), business.getLabel());
  }
}
