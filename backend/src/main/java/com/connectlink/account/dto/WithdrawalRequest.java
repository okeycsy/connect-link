package com.connectlink.account.dto;

import javax.validation.constraints.NotBlank;

/** 탈퇴. 본인인지 한 번 더 확인하려고 지금 비밀번호를 받는다. */
public record WithdrawalRequest(@NotBlank(message = "비밀번호를 입력해 주세요.") String password) {}
