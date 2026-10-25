package com.connectlink.notification;

/** 알림 종류. 기능 프로젝트가 새 알림을 만들면 여기에 항목을 더한다. 화면은 {@code label} 을 보여 준다. */
public enum NotificationType {
  SIGNUP_REQUESTED("가입 신청"),
  SIGNUP_APPROVED("가입 승인"),
  CONTACT_CHANGE_REQUESTED("연락처 변경 요청"),
  CONTACT_CHANGE_REMINDER("연락처 변경 리마인드"),
  CONTACT_RECONFIRM_DUE("연락처 확인 요청"),
  OPINION_REQUEST_OPENED("의견 요청 등록"),
  OPINION_REQUEST_CHANGED("의견 요청 변경"),
  OPINION_REPLY_DUE("회신 기한 임박"),
  OPINION_REPLY_REVISION("회신 수정 요청"),
  TEST_PLAN_OPENED("기관 테스트 등록"),
  TEST_RESULT_REPORTED("기관 테스트 결과 등록"),
  TEST_RESULT_FEEDBACK("기관 테스트 결제원 의견"),
  TEST_RESULT_COMMENT("기관 테스트 의견 답글"),
  TEST_RESULT_DUE("기관 테스트 수행 종료 임박"),
  TEST_DATA_REQUESTED("기관 테스트데이터 요청"),
  TEST_DATA_REPORTED("기관 테스트데이터 등록"),
  SUSPENSION_CHANGED("일시중단 변경"),
  SYSTEM("안내");

  private final String label;

  NotificationType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
