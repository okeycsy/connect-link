package com.connectlink.account.dto;

import com.connectlink.account.AccountStatus;
import com.connectlink.organization.dto.BusinessResponse;
import java.time.LocalDateTime;
import java.util.List;

/** 가입 신청을 받았다. 승인 대기 화면이 이 값을 보여 준다. */
public record SignUpResponse(
    String email,
    String name,
    String organizationName,
    String organizationTypeLabel,
    List<BusinessResponse> businesses,
    String jobRoleLabel,
    AccountStatus status,
    String statusLabel,
    LocalDateTime appliedAt) {}
