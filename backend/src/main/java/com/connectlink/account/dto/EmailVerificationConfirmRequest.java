package com.connectlink.account.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** 받은 인증 코드 확인. */
public record EmailVerificationConfirmRequest(
    @NotBlank(message = "회사 이메일을 입력해 주세요.") String email,
    @NotBlank(message = "인증 코드를 입력해 주세요.")
        @Pattern(regexp = "^$|^\\d{6}$", message = "인증 코드는 숫자 6자리입니다.")
        String code) {}
