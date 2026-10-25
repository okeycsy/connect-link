package com.connectlink.organization;

/** 기관 한눈에 보기의 칸. 화면에 이 순서로 보인다. 기능 모듈은 자기 칸에 {@link OrganizationOverviewContributor} 를 하나 만든다. */
public enum OrganizationOverviewSectionKey {
  CONTACT("담당자", "contacts"),
  OPINION_REQUEST("회신을 기다리는 의견 요청", "opinions"),
  TEST_PLAN("진행 중 기관 테스트", "tests"),
  SUSPENSION("일시중단", "suspensions");

  private final String title;
  private final String feature;

  OrganizationOverviewSectionKey(String title, String feature) {
    this.title = title;
    this.feature = feature;
  }

  public String getTitle() {
    return title;
  }

  /** 그 기능 목록 화면의 주소 조각(예: opinions). */
  public String getFeature() {
    return feature;
  }
}
