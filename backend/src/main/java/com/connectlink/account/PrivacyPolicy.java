package com.connectlink.account;

/**
 * 개인정보 처리방침의 판. 가입 때 확인한 수집·이용 안내와 선택 동의가 어느 처리방침에 대한 것인지 계정에 남긴다. 처리방침 본문은 화면(frontend 의 {@code
 * privacyPolicy.ts})에 있고, 그 시행일과 이 값이 같아야 한다. 처리방침을 바꾸면 둘을 함께 고친다.
 */
public final class PrivacyPolicy {

  /** 지금 처리방침의 시행일. */
  public static final String VERSION = "2026-10-01";

  private PrivacyPolicy() {}
}
