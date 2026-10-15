package com.connectlink.auth;

import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 데모·개발 프로필에서 로그인 없이 들어온 요청을 데모 관리자로 인증한다.
 *
 * <p>어드민 전제로 공통 화면을 먼저 만들기로 한 결정(docs/decisions.md 2026-10-15)에 따른 것이다. 운영 프로필에는 등록되지 않으므로 로그인 API
 * 가 생기기 전까지 운영에서는 보호된 API 가 401 을 돌려준다. 계정 모듈이 붙으면 이 필터는 데모 계정 전환 도구로만 남긴다.
 */
@Component
@Profile({"local", "demo"})
public class DemoAuthenticationFilter extends OncePerRequestFilter {

  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication existing = SecurityContextHolder.getContext().getAuthentication();
    if (existing == null || !(existing.getPrincipal() instanceof CurrentUser)) {
      SecurityContextHolder.getContext().setAuthentication(authenticationFor(DemoUsers.ADMIN));
    }
    chain.doFilter(request, response);
  }

  /** 테스트와 데모 필터가 같은 모양의 인증 객체를 쓰도록 한곳에서 만든다. */
  public static Authentication authenticationFor(CurrentUser user) {
    List<SimpleGrantedAuthority> authorities =
        user.admin() ? List.of(new SimpleGrantedAuthority(ROLE_ADMIN)) : List.of();
    return new UsernamePasswordAuthenticationToken(user, null, authorities);
  }
}
