package com.connectlink.common;

import org.springframework.http.HttpStatus;

/** 현재 요청의 망을 판단한다. 업무 코드는 IP 나 헤더를 직접 보지 않고 이 인터페이스만 쓴다. */
public interface NetworkZoneResolver {

  /** 내부망 전용 기능이 보내는 오류 코드. 화면은 이 코드로 "내부망 전용" 안내를 띄운다. */
  String INTERNAL_ONLY = "INTERNAL_ONLY";

  NetworkZone current();

  default boolean isInternal() {
    return current() == NetworkZone.INTERNAL;
  }

  /** 내부망이 아니면 403(INTERNAL_ONLY). {@code feature} 는 안내문에 들어갈 기능 이름(예: "엑셀 다운로드"). */
  default void ensureInternal(String feature) {
    if (!isInternal()) {
      throw new BusinessException(
          HttpStatus.FORBIDDEN, INTERNAL_ONLY, feature + "은(는) 내부망에서만 쓸 수 있습니다.");
    }
  }
}
