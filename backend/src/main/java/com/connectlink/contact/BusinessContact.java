package com.connectlink.contact;

import com.connectlink.organization.JobRole;

/**
 * 한 업무의 담당자 연락처 한 명. 다른 모듈이 기관 담당자를 고를 때(예: 의견 요청의 회신 담당자) 쓰는 읽기 전용 값이다. 계정이 없는 담당자(후임자 등)도 들어 있다.
 *
 * @param contactId 담당자 연락처 ID
 * @param email 회사 이메일(소문자). 계정과 잇는 키다
 * @param subBusiness 세부업무. 없으면 null
 * @param jobRoleLabel 직무 표기(예: "전산(개발)")
 */
public record BusinessContact(
    Long contactId,
    Long organizationId,
    String email,
    String name,
    String department,
    String team,
    String jobTitle,
    JobRole jobRole,
    String jobRoleLabel,
    ContactRank rank,
    String subBusiness) {}
