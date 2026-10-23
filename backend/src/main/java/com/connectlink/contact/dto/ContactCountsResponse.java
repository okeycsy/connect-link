package com.connectlink.contact.dto;

/**
 * 목록 머리의 건수. 사용자가 볼 수 있는 범위 안이다.
 *
 * @param needsConfirm 최근 확인일에서 6개월이 지난 담당자 수
 * @param unreachable 연락 불가로 표시된 담당자 수
 */
public record ContactCountsResponse(long total, long needsConfirm, long unreachable) {}
