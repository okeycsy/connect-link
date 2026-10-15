package com.connectlink.auth;

import com.connectlink.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * 보안 기본값. 세션(쿠키) 인증, CSRF 쿠키 토큰, 화면·공개 기준정보·API 문서만 열어 두고 나머지 API는 로그인 필수. 로그인 API 자체는 account 모듈과
 * 함께 다음 단계에서 붙는다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  public SecurityConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringAntMatchers("/h2-console/**"))
        .headers(headers -> headers.frameOptions().sameOrigin())
        .authorizeRequests(
            auth ->
                auth
                    // 화면 자원과 화면 라우트
                    .antMatchers("/", "/index.html", "/assets/**", "/favicon.svg")
                    .permitAll()
                    .regexMatchers("/(?!api/)[^.]*")
                    .permitAll()
                    // 공개 API: 상태, 가입 화면이 쓰는 기준정보, API 문서
                    .antMatchers(
                        "/api/v1/health",
                        "/api/v1/businesses",
                        "/api/v1/organizations",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/h2-console/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonUnauthorizedEntryPoint()))
        .formLogin()
        .disable()
        .httpBasic()
        .disable()
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessHandler(
                        (request, response, authentication) ->
                            response.setStatus(HttpStatus.NO_CONTENT.value())));
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
