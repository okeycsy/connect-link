package com.connectlink.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class DemoAuthenticationFilterTest {

  private final DemoAuthenticationFilter filter = new DemoAuthenticationFilter();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 로그인_없는_요청은_데모_관리자로_인증된다() throws Exception {
    filter.doFilter(
        new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth.getPrincipal()).isEqualTo(DemoUsers.ADMIN);
    assertThat(auth.getName()).isEqualTo("admin@kftc.example.com");
    assertThat(auth.getAuthorities())
        .extracting("authority")
        .containsExactly(DemoAuthenticationFilter.ROLE_ADMIN);
  }

  @Test
  void 이미_로그인된_요청은_바꾸지_않는다() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(DemoAuthenticationFilter.authenticationFor(TestUsers.GANA_MEMBER));

    filter.doFilter(
        new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isEqualTo(TestUsers.GANA_MEMBER);
  }
}
