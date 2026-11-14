package com.connectlink.auth.audit;

import com.connectlink.auth.CurrentUser;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 개인정보를 다루는 요청({@link AccessLogRule})이 끝나면 접속기록을 남긴다. Spring Security 필터 뒤에서 돌아 로그인 사용자를 안다. 로그인하지
 * 않은 요청은 남기지 않는다(로그인 실패는 계정 잠금이 센다). 기록을 남기지 못해도 요청은 그대로 끝낸다.
 */
@Component
public class AccessLogFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

  private final AccessLogService service;

  public AccessLogFilter(AccessLogService service) {
    this.service = service;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Optional<AccessLogRule> rule = AccessLogRule.match(request.getMethod(), pathOf(request));
    try {
      chain.doFilter(request, response);
    } finally {
      rule.ifPresent(r -> record(r, request, response));
    }
  }

  private void record(
      AccessLogRule rule, HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof CurrentUser user)) {
      return;
    }
    try {
      service.record(
          user,
          request.getRemoteAddr(),
          rule,
          request.getMethod(),
          pathOf(request),
          request.getQueryString(),
          response.getStatus());
    } catch (RuntimeException e) {
      log.error("접속기록을 남기지 못했습니다: {} {}", request.getMethod(), request.getRequestURI(), e);
    }
  }

  private static String pathOf(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String context = request.getContextPath();
    return context != null && !context.isEmpty() && uri.startsWith(context)
        ? uri.substring(context.length())
        : uri;
  }
}
