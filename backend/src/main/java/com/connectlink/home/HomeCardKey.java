package com.connectlink.home;

/**
 * 홈 "지금 할 일" 카드 자리. 순서가 화면 순서다. 각 기능 프로젝트는 자기 자리의 {@link HomeCardContributor} 를 구현해 끼운다.
 *
 * <p>{@code feature} 는 카드의 "모두 보기" 가 여는 업무 영역 아래 경로다.
 */
public enum HomeCardKey {
  CONTACT_CONFIRM("확인이 필요한 담당자", "contacts"),
  UPDATE_CAMPAIGN("정기 변경 진행률", "contacts"),
  OPINION_REPLIES("회신 대기 의견 요청", "opinions"),
  TEST_PROGRESS("진행 중 기관 테스트", "tests"),
  SUSPENSIONS("이번 주 일시중단", "suspensions"),
  SIGNUP_APPROVAL("가입 승인 대기", "admin/sign-ups");

  private final String title;
  private final String feature;

  HomeCardKey(String title, String feature) {
    this.title = title;
    this.feature = feature;
  }

  public String getTitle() {
    return title;
  }

  public String getFeature() {
    return feature;
  }
}
