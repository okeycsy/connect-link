package com.connectlink.contact.dto;

import com.connectlink.contact.ContactRank;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 담당자 한 명. 목록 한 줄과 상세 패널이 같은 값을 쓴다.
 *
 * @param lastConfirmedAt 최근 확인일. 고치거나 "변동 없음" 으로 확인한 때다
 * @param needsConfirm 최근 확인일에서 6개월이 지났는지
 * @param self 로그인한 사람 본인인지(회사 이메일이 같다)
 */
public record ContactResponse(
    Long id,
    Business business,
    String businessLabel,
    Long organizationId,
    String organizationCode,
    String organizationName,
    OrganizationType organizationType,
    @Schema(nullable = true) String subBusiness,
    String name,
    String department,
    @Schema(nullable = true) String team,
    @Schema(nullable = true) String jobTitle,
    JobRole jobRole,
    String jobRoleLabel,
    List<ItRole> itRoles,
    ContactRank rank,
    String rankLabel,
    String extensionPhone,
    @Schema(nullable = true) String mobilePhone,
    String email,
    LocalDateTime lastConfirmedAt,
    String lastConfirmedByName,
    boolean needsConfirm,
    boolean self,
    ContactPermissions permissions) {}
