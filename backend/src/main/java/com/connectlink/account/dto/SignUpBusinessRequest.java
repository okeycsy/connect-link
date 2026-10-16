package com.connectlink.account.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 가입 때 고른 담당 업무 한 건과 세부업무(자유 텍스트, 선택). */
public record SignUpBusinessRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @Schema(nullable = true) @Size(max = 200, message = "세부업무는 200자까지 쓸 수 있습니다.")
        String subBusiness) {}
