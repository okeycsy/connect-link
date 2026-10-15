package com.connectlink.common;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 모든 테이블의 created_by, updated_by 를 로그인 사용자 이름으로 채운다. 로그인 전이면 "system". */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

  public static final String SYSTEM_AUDITOR = "system";

  @Bean
  public AuditorAware<String> auditorAware() {
    return () -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
        return Optional.of(SYSTEM_AUDITOR);
      }
      return Optional.of(auth.getName());
    };
  }
}
