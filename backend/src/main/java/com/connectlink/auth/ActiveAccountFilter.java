package com.connectlink.auth;

import com.connectlink.account.AccountService;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 세션의 로그인 사용자가 아직 사용 중 계정인지 API 요청마다 확인한다. 다른 브라우저에서 탈퇴했으면 이 세션도 끝내고 로그인 전 요청으로 다룬다(로그인이 필요한 API 는
 * 401). 빈으로 두지 않는다(서블릿 필터로 한 번 더 등록되지 않게 {@link SecurityConfig} 가 직접 만든다).
 */
class ActiveAccountFilter extends OncePerRequestFilter {

  private final AccountService accountService;

  ActiveAccountFilter(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof CurrentUser user
        && !accountService.isActive(user.email())) {
      new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
    chain.doFilter(request, response);
  }
}
