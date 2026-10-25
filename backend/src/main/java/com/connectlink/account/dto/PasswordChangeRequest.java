package com.connectlink.account.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** 내 정보 > 비밀번호 변경. 본인 확인으로 지금 비밀번호를 받는다. 새 비밀번호 규칙은 가입과 같다. */
public record PasswordChangeRequest(
    @NotBlank(message = "지금 비밀번호를 입력해 주세요.") String currentPassword,
    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
        @Pattern(regexp = SignUpRequest.PASSWORD_RULE, message = SignUpRequest.PASSWORD_MESSAGE)
        String newPassword) {}
