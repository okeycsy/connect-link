package com.connectlink.home;

/** 카드 내용의 출처. 화면은 {@code SAMPLE} 에 "샘플" 표시를, {@code UNAVAILABLE} 에 "연결 예정" 안내를 보인다. */
public enum HomeCardSource {
  /** 기능 모듈의 실제 데이터. */
  REAL,
  /** 기능 프로젝트가 붙기 전까지 공통이 넣어 둔 샘플(local·demo 프로필). */
  SAMPLE,
  /** 이 자리를 채우는 기여자가 없다. */
  UNAVAILABLE
}
