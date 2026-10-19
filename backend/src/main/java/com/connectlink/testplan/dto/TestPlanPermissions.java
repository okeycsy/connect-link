package com.connectlink.testplan.dto;

/**
 * 지금 사용자가 이 기관 테스트에 할 수 있는 일. 화면은 이 값으로 버튼을 보이고, 서버는 같은 규칙으로 다시 막는다.
 *
 * @param manage 결제원 쪽 화면(전 기관 현황, 알림 설정, 변경 이력)을 쓰는지. 고치기·지우기·가이드 첨부도 이 값이다
 * @param report 자기 기관 결과를 등록할 수 있는지(대상 기관 담당자)
 * @param feedback 기관 결과에 결제원 의견을 남길 수 있는지
 */
public record TestPlanPermissions(boolean manage, boolean report, boolean feedback) {}
