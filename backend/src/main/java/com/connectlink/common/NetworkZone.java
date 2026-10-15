package com.connectlink.common;

/** 요청이 들어온 망. 내부망 전용 기능(연락처 수정·삭제, 검색·필터, 엑셀 다운로드 등)은 {@link NetworkZoneResolver} 로만 판단한다. */
public enum NetworkZone {
  INTERNAL("내부망"),
  EXTERNAL("인터넷망");

  private final String label;

  NetworkZone(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
