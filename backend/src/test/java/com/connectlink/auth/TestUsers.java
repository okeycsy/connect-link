package com.connectlink.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.connectlink.organization.OrganizationType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** MockMvc 요청에 로그인 사용자를 싣는다. 권한 테스트는 허용 사용자와 거부 사용자를 하나씩 쓴다. */
public final class TestUsers {

  /** 결제원 데모 관리자(전 업무, 관리자). */
  public static final CurrentUser ADMIN = DemoUsers.ADMIN;

  /** 가나은행 계좌통합관리 담당자(관리자 아님). 알림 시드에 이 사람 앞으로 1건이 있다. */
  public static final CurrentUser GANA_MEMBER =
      new CurrentUser(
          "kim.gana@gana.example.com",
          "김가나",
          "GANA",
          "가나은행",
          OrganizationType.FINANCE_ORGANIZATION,
          false);

  private TestUsers() {}

  public static RequestPostProcessor admin() {
    return as(ADMIN);
  }

  public static RequestPostProcessor ganaMember() {
    return as(GANA_MEMBER);
  }

  public static RequestPostProcessor as(CurrentUser user) {
    return authentication(DemoAuthenticationFilter.authenticationFor(user));
  }
}
