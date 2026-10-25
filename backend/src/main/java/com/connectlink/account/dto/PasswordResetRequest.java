package com.connectlink.account.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 비밀번호 찾기 코드 받기. 가입한 회사 이메일. */
public record PasswordResetRequest(
    @NotBlank(message = "회사 이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자까지 쓸 수 있습니다.")
        String email) {}
