package com.connectlink.auth.dto;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.NetworkZone;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import com.connectlink.organization.dto.BusinessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Comparator;
import java.util.List;

/** 로그인 사용자와 지금 접속한 망. 담당 업무는 업무 영역 순서(enum 순)다. 직급은 글쓴이 표시("결제원(김민찬 과장)")에 쓴다. */
public record CurrentUserResponse(
    String email,
    String name,
    @Schema(nullable = true) String jobTitle,
    String organizationCode,
    String organizationName,
    OrganizationType organizationType,
    String organizationTypeLabel,
    boolean admin,
    List<BusinessResponse> businesses,
    JobRole jobRole,
    String jobRoleLabel,
    List<ItRole> itRoles,
    String roleLabel,
    NetworkZone networkZone,
    String networkZoneLabel) {

  public static CurrentUserResponse from(CurrentUser user, NetworkZone zone) {
    return new CurrentUserResponse(
        user.email(),
        user.name(),
        user.jobTitle(),
        user.organizationCode(),
        user.organizationName(),
        user.organizationType(),
        user.organizationType().getLabel(),
        user.admin(),
        user.businesses().stream()
            .sorted(Comparator.naturalOrder())
            .map(BusinessResponse::from)
            .toList(),
        user.jobRole(),
        user.jobRole().describe(user.itRoles()),
        user.itRoles().stream().sorted(Comparator.<ItRole>naturalOrder()).toList(),
        user.roleLabel(),
        zone,
        zone.getLabel());
  }
}
