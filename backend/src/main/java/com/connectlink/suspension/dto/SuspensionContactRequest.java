package com.connectlink.suspension.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 일시중단 양식의 기관 작업 담당자 한 명. */
public record SuspensionContactRequest(
    @NotBlank(message = "담당자 이름을 입력해 주세요.") @Size(max = 50, message = "담당자 이름은 50자까지입니다.")
        String name,
    @NotBlank(message = "담당자 연락처를 입력해 주세요.")
        @Pattern(regexp = "^[0-9][0-9-]{6,18}[0-9]$", message = "연락처는 숫자와 - 로 입력해 주세요.")
        String phone,
    @Schema(nullable = true)
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자까지입니다.")
        String email) {}
