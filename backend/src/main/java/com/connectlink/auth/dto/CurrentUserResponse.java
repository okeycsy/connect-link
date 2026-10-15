package com.connectlink.auth.dto;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.NetworkZone;
import com.connectlink.organization.OrganizationType;

/** 로그인 사용자와 지금 접속한 망. */
public record CurrentUserResponse(
    String email,
    String name,
    String organizationCode,
    String organizationName,
    OrganizationType organizationType,
    String organizationTypeLabel,
    boolean admin,
    NetworkZone networkZone,
    String networkZoneLabel) {

  public static CurrentUserResponse from(CurrentUser user, NetworkZone zone) {
    return new CurrentUserResponse(
        user.email(),
        user.name(),
        user.organizationCode(),
        user.organizationName(),
        user.organizationType(),
        user.organizationType().getLabel(),
        user.admin(),
        zone,
        zone.getLabel());
  }
}
