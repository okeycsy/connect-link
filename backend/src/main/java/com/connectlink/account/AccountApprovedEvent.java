package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import java.util.Map;
import java.util.Set;

/**
 * 가입 신청이 승인되어 계정이 사용 중이 되었다. 승인과 같은 트랜잭션 안에서 발행하므로 기능 모듈은 {@code @EventListener} 로 받아 자기 데이터(예: 연락처
 * 모듈의 담당자 행)를 만든다. 받는 쪽이 실패하면 승인도 되돌아간다. 가입 때 받은 부서·팀·직급·내선·휴대폰을 함께 넘긴다(docs/decisions.md
 * 2026-10-15 "승인 뒤 연락처 모듈이 넘겨받는다").
 *
 * @param approvedBy 승인한 결제원 담당자(또는 관리자)
 * @param subBusinesses 담당 업무별 세부업무. 세부업무가 없는 업무는 값이 빈 문자열이 아니라 키만 있고 값이 null 이다
 */
public record AccountApprovedEvent(
    Long accountId,
    String email,
    String name,
    Long organizationId,
    String organizationCode,
    String department,
    String team,
    String jobTitle,
    JobRole jobRole,
    Set<ItRole> itRoles,
    String extensionPhone,
    String mobilePhone,
    Map<Business, String> subBusinesses,
    CurrentUser approvedBy) {}
