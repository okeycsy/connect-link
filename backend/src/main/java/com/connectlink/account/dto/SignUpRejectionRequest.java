package com.connectlink.account.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 가입 반려. 사유는 신청자에게 가는 반려 메일에 그대로 들어간다. */
public record SignUpRejectionRequest(
    @NotBlank(message = "반려 사유를 적어 주세요.") @Size(max = 500, message = "반려 사유는 500자까지 적을 수 있습니다.")
        String reason) {}
