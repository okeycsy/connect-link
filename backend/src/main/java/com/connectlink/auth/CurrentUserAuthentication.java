package com.connectlink.auth;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** {@link CurrentUser} 를 principal 로 하는 인증 객체. 로그인 API 와 테스트가 같은 모양을 쓰도록 한곳에서 만든다. */
public final class CurrentUserAuthentication {

  public static final String ROLE_ADMIN = "ROLE_ADMIN";
  public static final String ROLE_USER = "ROLE_USER";

  private CurrentUserAuthentication() {}

  public static Authentication of(CurrentUser user) {
    List<SimpleGrantedAuthority> authorities =
        user.admin()
            ? List.of(new SimpleGrantedAuthority(ROLE_USER), new SimpleGrantedAuthority(ROLE_ADMIN))
            : List.of(new SimpleGrantedAuthority(ROLE_USER));
    return new UsernamePasswordAuthenticationToken(user, null, authorities);
  }
}
