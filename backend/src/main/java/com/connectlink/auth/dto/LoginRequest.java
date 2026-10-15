package com.connectlink.auth.dto;

import javax.validation.constraints.NotBlank;

/** 로그인 요청. 이메일은 대소문자와 앞뒤 공백을 가리지 않는다. */
public record LoginRequest(
    @NotBlank(message = "회사 이메일을 입력해 주세요.") String email,
    @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {}
