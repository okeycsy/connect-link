package com.connectlink.account.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 인증 코드 받기. 이메일은 대소문자와 앞뒤 공백을 가리지 않는다. */
public record EmailVerificationRequest(
    @NotBlank(message = "회사 이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자까지 쓸 수 있습니다.")
        String email) {}
