package com.connectlink.auth;

import com.connectlink.organization.OrganizationType;

/** 데모 프로필의 고정 사용자. 계정 모듈이 붙으면 샘플 계정 테이블로 옮긴다. 전부 가상 데이터다. */
public final class DemoUsers {

  /** 어드민 전제로 공통 화면을 만드는 동안 모든 요청이 이 사용자로 들어온다. */
  public static final CurrentUser ADMIN =
      new CurrentUser(
          "admin@kftc.example.com", "데모 관리자", "KFTC", "금융결제원", OrganizationType.KFTC, true);

  private DemoUsers() {}
}
