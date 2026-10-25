package com.connectlink.auth;

import com.connectlink.account.AccountService;
import com.connectlink.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * 보안 기본값. 세션(쿠키) 인증, CSRF 쿠키 토큰, 화면·공개 기준정보·로그인·가입·API 문서만 열어 두고 나머지 API는 로그인 필수. 로그인과 로그아웃은 {@link
 * AuthController} 가 한다(폼 로그인·기본 로그아웃 필터는 끈다). 탈퇴한 계정의 다른 세션은 {@link ActiveAccountFilter} 가 끝낸다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, AccountService accountService)
      throws Exception {
    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringAntMatchers("/h2-console/**"))
        .headers(headers -> headers.frameOptions().sameOrigin())
        .authorizeRequests(
            auth ->
                auth
                    // 화면 자원과 화면 라우트
                    .antMatchers("/", "/index.html", "/assets/**", "/favicon.png")
                    .permitAll()
                    .regexMatchers("/(?!api/)[^.]*")
                    .permitAll()
                    // 공개 API: 상태, 가입 화면이 쓰는 기준정보(조회만), 로그인·로그아웃, 가입, 비밀번호 찾기, API 문서
                    .antMatchers(
                        HttpMethod.GET,
                        "/api/v1/businesses",
                        "/api/v1/job-roles",
                        "/api/v1/organizations")
                    .permitAll()
                    .antMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/login",
                        "/api/v1/auth/logout",
                        "/api/v1/email-verifications",
                        "/api/v1/email-verifications/confirm",
                        "/api/v1/sign-ups",
                        "/api/v1/password-resets",
                        "/api/v1/password-resets/confirm")
                    .permitAll()
                    // 데모 계정 목록(로그인 화면)과 받은 메일함(가입 화면). 운영 프로필에는 이 API 가 없어 404 다.
                    .antMatchers(HttpMethod.GET, "/api/v1/demo/accounts", "/api/v1/demo/mailbox")
                    .permitAll()
                    .antMatchers(
                        "/api/v1/health",
                        "/api/v1/support-info",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/h2-console/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(
            new ActiveAccountFilter(accountService), AnonymousAuthenticationFilter.class)
        .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonUnauthorizedEntryPoint()))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 로그인 안 된 API 호출에는 로그인 페이지로 보내지 않고 401 JSON을 돌려준다. */
  private AuthenticationEntryPoint jsonUnauthorizedEntryPoint() {
    return (request, response, authException) -> {
      ErrorResponse body =
          new ErrorResponse(
              "about:blank",
              HttpStatus.UNAUTHORIZED.getReasonPhrase(),
              HttpStatus.UNAUTHORIZED.value(),
              "로그인이 필요합니다.",
              request.getRequestURI(),
              "UNAUTHORIZED",
              List.of(),
              OffsetDateTime.now());
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      objectMapper.writeValue(response.getWriter(), body);
    };
  }
}
