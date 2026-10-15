package com.connectlink.auth;

import com.connectlink.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Spring Security 세션의 principal 에서 {@link CurrentUser} 를 꺼낸다. */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

  @Override
  public CurrentUser require() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CurrentUser user) {
      return user;
    }
    throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
  }
}
