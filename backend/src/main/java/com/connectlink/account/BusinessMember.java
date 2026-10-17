package com.connectlink.account;

import com.connectlink.organization.JobRole;

/**
 * 한 업무를 맡은 사용 중 계정 한 명. 다른 모듈이 기관 담당자를 고를 때(예: 의견 요청의 회신 담당자) 쓰는 읽기 전용 값이다.
 *
 * @param accountId 계정 ID
 * @param subBusiness 그 업무의 세부업무. 없으면 null
 * @param jobRoleLabel 직무 표기(예: "전산(개발)")
 */
public record BusinessMember(
    Long accountId,
    String email,
    String name,
    Long organizationId,
    String organizationCode,
    String department,
    String team,
    String jobTitle,
    JobRole jobRole,
    String jobRoleLabel,
    String subBusiness) {}
