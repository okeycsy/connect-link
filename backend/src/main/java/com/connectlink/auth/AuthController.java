package com.connectlink.auth;

import com.connectlink.account.AccountService;
import com.connectlink.auth.dto.CurrentUserResponse;
import com.connectlink.auth.dto.LoginRequest;
import com.connectlink.common.NetworkZoneResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인·로그아웃. 세션(쿠키) 방식이다. 로그인하면 세션 ID 를 새로 발급해(세션 고정 방지) 로그인 사용자를 세션에 두고, 로그아웃하면 세션을 버린다. 둘 다 CSRF
 * 토큰(XSRF-TOKEN 쿠키 → X-XSRF-TOKEN 헤더)이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "계정", description = "로그인 사용자")
public class AuthController {

  private final AccountService accountService;
  private final NetworkZoneResolver networkZoneResolver;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();
  private final SessionAuthenticationStrategy sessionStrategy =
      new ChangeSessionIdAuthenticationStrategy();

  public AuthController(AccountService accountService, NetworkZoneResolver networkZoneResolver) {
    this.accountService = accountService;
    this.networkZoneResolver = networkZoneResolver;
  }

  @PostMapping("/login")
  @Operation(
      summary = "로그인",
      description =
          "회사 이메일과 비밀번호로 로그인한다. 틀리면 401 INVALID_CREDENTIALS, 승인 전이면 403 ACCOUNT_PENDING_APPROVAL, 반려면 403"
              + " ACCOUNT_REJECTED.")
  public CurrentUserResponse login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    CurrentUser user = accountService.authenticate(request.email(), request.password());
    Authentication authentication = CurrentUserAuthentication.of(user);
    sessionStrategy.onAuthentication(authentication, httpRequest, httpResponse);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);
    return CurrentUserResponse.from(user, networkZoneResolver.current());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "로그아웃", description = "세션을 버린다. 로그인하지 않은 상태에서 불러도 204 다.")
  public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    new SecurityContextLogoutHandler()
        .logout(httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());
  }
}
