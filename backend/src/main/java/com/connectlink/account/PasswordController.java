package com.connectlink.account;

import com.connectlink.account.dto.PasswordChangeRequest;
import com.connectlink.account.dto.PasswordResetConfirmRequest;
import com.connectlink.account.dto.PasswordResetRequest;
import com.connectlink.account.dto.PasswordResetResponse;
import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.auth.LoginSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 비밀번호 변경(로그인한 본인)과 비밀번호 찾기(로그인 전, CSRF 토큰은 필요하다). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "계정", description = "로그인 사용자")
public class PasswordController {

  private final PasswordService passwordService;
  private final CurrentUserProvider currentUserProvider;

  public PasswordController(
      PasswordService passwordService, CurrentUserProvider currentUserProvider) {
    this.passwordService = passwordService;
    this.currentUserProvider = currentUserProvider;
  }

  @PutMapping("/me/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "비밀번호 변경",
      description =
          "지금 비밀번호를 확인하고 새 비밀번호로 바꾼다. 이 세션은 남고 다른 곳의 세션은 끝난다. 지금 비밀번호가 틀리면 400 PASSWORD_MISMATCH, 같은"
              + " 비밀번호면 400 PASSWORD_UNCHANGED.")
  public void change(
      @Valid @RequestBody PasswordChangeRequest request, HttpServletRequest httpRequest) {
    var changedAt =
        passwordService.change(
            currentUserProvider.require(), request.currentPassword(), request.newPassword());
    LoginSession.markLogin(httpRequest, changedAt);
  }

  @PostMapping("/password-resets")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
      summary = "비밀번호 찾기 코드 받기",
      description =
          "사용 중 계정의 회사 이메일로 숫자 6자리 코드를 보낸다(계정 유무와 상관없이 같은 답). 재발송 간격 전이면 429"
              + " VERIFICATION_RESEND_TOO_SOON.")
  public PasswordResetResponse request(@Valid @RequestBody PasswordResetRequest request) {
    return passwordService.request(request.email());
  }

  @PostMapping("/password-resets/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "비밀번호 찾기: 코드 확인과 새 비밀번호",
      description =
          "코드가 맞으면 새 비밀번호로 바꾸고 그 계정의 모든 세션을 끝낸다. 틀리면 400 VERIFICATION_CODE_MISMATCH, 정해진 횟수를 넘기면"
              + " VERIFICATION_ATTEMPTS_EXCEEDED, 유효 시간이 지났으면 VERIFICATION_EXPIRED.")
  public void confirm(@Valid @RequestBody PasswordResetConfirmRequest request) {
    passwordService.confirm(request.email(), request.code(), request.newPassword());
  }
}
