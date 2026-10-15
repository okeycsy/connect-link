package com.connectlink.organization.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 기관 수정 요청. 이름, 이메일 도메인, 사용 여부, 참여 업무를 통째로 바꾼다. */
public record OrganizationUpdateRequest(
    @NotBlank(message = "기관명을 입력해 주세요.") @Size(max = 100, message = "기관명은 100자까지입니다.") String name,
    @Schema(nullable = true)
        @Pattern(
            regexp = "^\\s*$|^[a-z0-9.-]+\\.[a-z]{2,}$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "이메일 도메인은 example.com 같은 형식입니다.")
        @Size(max = 100, message = "이메일 도메인은 100자까지입니다.")
        String emailDomain,
    @NotNull(message = "사용 여부를 골라 주세요.") Boolean active,
    @NotNull(message = "참여 업무를 골라 주세요.") Set<Business> businesses) {}
