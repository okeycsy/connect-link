package com.connectlink.account;

import com.connectlink.account.dto.EmailVerificationConfirmRequest;
import com.connectlink.account.dto.EmailVerificationConfirmResponse;
import com.connectlink.account.dto.EmailVerificationRequest;
import com.connectlink.account.dto.EmailVerificationResponse;
import com.connectlink.account.dto.SignUpRequest;
import com.connectlink.account.dto.SignUpResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 회원가입: 회사 이메일 인증과 가입 신청. 로그인 전에 쓰는 API 라 모두 열려 있고, CSRF 토큰은 필요하다. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "가입", description = "회사 이메일 인증과 가입 신청")
public class SignUpController {

  private final EmailVerificationService verificationService;
  private final SignUpService signUpService;

  public SignUpController(
      EmailVerificationService verificationService, SignUpService signUpService) {
    this.verificationService = verificationService;
    this.signUpService = signUpService;
  }

  @PostMapping("/email-verifications")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "회사 이메일 인증 코드 받기",
      description =
          "숫자 6자리 코드를 메일로 보낸다(프로토타입은 발송함에 기록). 이미 계정이 있으면 409 EMAIL_ALREADY_REGISTERED, 재발송 간격 전이면 429"
              + " VERIFICATION_RESEND_TOO_SOON.")
  public EmailVerificationResponse request(@Valid @RequestBody EmailVerificationRequest request) {
    return verificationService.request(request.email());
  }

  @PostMapping("/email-verifications/confirm")
  @Operation(
      summary = "회사 이메일 인증 코드 확인",
      description =
          "맞으면 가입 신청에 쓸 인증 토큰을 준다. 틀리면 400 VERIFICATION_CODE_MISMATCH, 정해진 횟수를 넘기면"
              + " VERIFICATION_ATTEMPTS_EXCEEDED, 유효 시간이 지나면 VERIFICATION_EXPIRED.")
  public EmailVerificationConfirmResponse confirm(
      @Valid @RequestBody EmailVerificationConfirmRequest request) {
    return verificationService.confirm(request.email(), request.code());
  }

  @PostMapping("/sign-ups")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "가입 신청",
      description =
          "인증 토큰과 가입 정보를 받아 승인 대기 계정을 만든다. 토큰이 없거나 만료·사용됐으면 400 VERIFICATION_REQUIRED, 기관이 참여하지 않는"
              + " 업무면 400 BUSINESS_NOT_PARTICIPATED.")
  public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
    return signUpService.signUp(request);
  }
}
