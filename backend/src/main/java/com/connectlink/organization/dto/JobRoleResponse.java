package com.connectlink.organization.dto;

import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import java.util.Arrays;
import java.util.List;

/** 직무 한 건과 고를 수 있는 세부 직무. 현업은 세부 직무가 없다. */
public record JobRoleResponse(JobRole code, String label, List<ItRoleResponse> itRoles) {

  /** 전산 세부 직무 한 건. */
  public record ItRoleResponse(ItRole code, String label) {}

  public static JobRoleResponse from(JobRole jobRole) {
    List<ItRoleResponse> itRoles =
        jobRole == JobRole.IT
            ? Arrays.stream(ItRole.values()).map(r -> new ItRoleResponse(r, r.getLabel())).toList()
            : List.of();
    return new JobRoleResponse(jobRole, jobRole.getLabel(), itRoles);
  }
}
