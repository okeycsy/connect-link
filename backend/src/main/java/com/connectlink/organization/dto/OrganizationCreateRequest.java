package com.connectlink.organization.dto;

import com.connectlink.organization.Business;
import com.connectlink.organization.FinanceSector;
import com.connectlink.organization.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 기관 등록 요청. 코드와 구분은 등록 뒤 바꾸지 않는다. */
public record OrganizationCreateRequest(
    @NotBlank(message = "기관 코드를 입력해 주세요.")
        @Pattern(regexp = "[A-Z0-9_]{2,20}", message = "기관 코드는 영문 대문자·숫자·밑줄 2~20자입니다.")
        String code,
    @NotBlank(message = "기관명을 입력해 주세요.") @Size(max = 100, message = "기관명은 100자까지입니다.") String name,
    @NotNull(message = "기관 구분을 골라 주세요.") OrganizationType type,
    @Schema(nullable = true)
        @Pattern(
            regexp = "^\\s*$|^[a-z0-9.-]+\\.[a-z]{2,}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "이메일 도메인은 example.com 같은 형식입니다.")
        @Size(max = 100, message = "이메일 도메인은 100자까지입니다.")
        String emailDomain,
    @NotNull(message = "참여 업무를 골라 주세요.") Set<Business> businesses,
    @Schema(nullable = true, description = "업권. 참가기관은 반드시 정하고 그 밖의 기관은 무시한다") FinanceSector sector) {}
