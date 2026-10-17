package com.connectlink.account.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 결제원 담당자의 탈퇴 처리. 사유는 탈퇴 처리된 담당자에게 가는 메일에 그대로 들어간다. */
public record AccountWithdrawalRequest(
    @NotBlank(message = "탈퇴 처리 사유를 적어 주세요.")
        @Size(max = 500, message = "탈퇴 처리 사유는 500자까지 적을 수 있습니다.")
        String reason) {}
