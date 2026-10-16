package com.connectlink.account.dto;

import com.connectlink.account.AccountStatus;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 가입 승인 화면의 신청 한 건. 승인하는 결제원 담당자가 소속과 이메일 도메인을 확인할 수 있도록 가입 때 적은 값을 그대로 보여 준다.
 *
 * @param id 계정 ID. 승인·반려 API 의 경로에 쓴다
 * @param emailDomainMatches 회사 이메일 도메인이 소속 기관에 등록된 도메인과 같은지
 * @param organizationEmailDomain 소속 기관에 등록된 이메일 도메인. 없으면 null
 * @param reviewedAt 승인·반려한 일시. 승인 대기면 null
 * @param reviewerName 승인·반려한 결제원 담당자. 승인 대기면 null
 * @param rejectionReason 반려 사유. 반려가 아니면 null
 */
public record SignUpApplicationResponse(
    Long id,
    String email,
    String name,
    Long organizationId,
    String organizationName,
    OrganizationType organizationType,
    String organizationTypeLabel,
    @Schema(nullable = true) String organizationEmailDomain,
    boolean emailDomainMatches,
    List<SignUpApplicationBusinessResponse> businesses,
    JobRole jobRole,
    String jobRoleLabel,
    String department,
    String team,
    String jobTitle,
    String extensionPhone,
    @Schema(nullable = true) String mobilePhone,
    AccountStatus status,
    String statusLabel,
    LocalDateTime appliedAt,
    @Schema(nullable = true) LocalDateTime reviewedAt,
    @Schema(nullable = true) String reviewerName,
    @Schema(nullable = true) String rejectionReason) {}
