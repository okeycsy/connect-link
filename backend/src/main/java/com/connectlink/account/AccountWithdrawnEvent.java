package com.connectlink.account;

import com.connectlink.organization.Business;
import java.util.Set;

/**
 * 계정이 탈퇴했다. 탈퇴와 같은 트랜잭션 안에서 발행하므로 기능 모듈은 {@code @EventListener} 로 받아 자기 데이터(예: 연락처의 본인 행)를 함께 지운다.
 * 받는 쪽이 실패하면 탈퇴도 되돌아간다.
 *
 * @param accountId 계정 ID. 계정 행은 탈퇴 뒤에도 남는다
 * @param email 탈퇴 전 회사 이메일. 계정 행에서는 이미 지웠다
 * @param organizationCode 소속 기관 코드
 * @param businesses 탈퇴 전 담당 업무
 * @param withdrawnByEmail 탈퇴 처리한 결제원 담당자의 이메일. 본인이 탈퇴했으면 null
 */
public record AccountWithdrawnEvent(
    Long accountId,
    String email,
    String organizationCode,
    Set<Business> businesses,
    String withdrawnByEmail) {}
