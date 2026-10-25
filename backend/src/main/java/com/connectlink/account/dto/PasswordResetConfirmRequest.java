package com.connectlink.account.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 비밀번호 찾기 코드를 확인하고 새 비밀번호로 바꾼다. 비밀번호 규칙은 가입과 같다. */
public record PasswordResetConfirmRequest(
    @NotBlank(message = "회사 이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자까지 쓸 수 있습니다.")
        String email,
    @NotBlank(message = "인증 코드를 입력해 주세요.")
        @Pattern(regexp = "^$|^[0-9]{6}$", message = "인증 코드는 숫자 6자리입니다.")
        String code,
    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Pattern(regexp = SignUpRequest.PASSWORD_RULE, message = SignUpRequest.PASSWORD_MESSAGE)
        String newPassword) {}
