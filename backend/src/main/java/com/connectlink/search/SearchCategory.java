package com.connectlink.search;

/** 전역 검색 결과의 묶음. 팔레트에 이 순서로 보인다. 기능 모듈은 자기 묶음에 {@link SearchProvider} 를 하나 만든다. */
public enum SearchCategory {
  ORGANIZATION("기관"),
  CONTACT("담당자"),
  OPINION_REQUEST("의견 요청"),
  TEST_PLAN("기관 테스트"),
  SUSPENSION("일시중단");

  private final String label;

  SearchCategory(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
