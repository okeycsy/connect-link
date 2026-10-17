package com.connectlink.suspension.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 일시중단 등록·수정 양식. 등록할 때 결제원 담당자는 기관을 고르고, 기관 담당자는 자기 기관으로 정해진다({@code organizationId} 를 무시한다). 수정할 때
 * 기관은 바뀌지 않는다.
 *
 * @param kftcAction 결제원 조치사항(중단 동안 결제원이 할 일)
 * @param contactName 기관 작업 담당자 이름
 */
public record SuspensionSaveRequest(
    @NotNull(message = "대상 업무를 골라 주세요.") Business business,
    @Schema(nullable = true) Long organizationId,
    @Schema(nullable = true) @Size(max = 200, message = "세부업무는 200자까지입니다.") String subBusiness,
    @NotNull(message = "중단 시작 일시를 골라 주세요.") LocalDateTime startAt,
    @NotNull(message = "중단 종료 일시를 골라 주세요.") LocalDateTime endAt,
    @NotBlank(message = "중단 이유(작업 내용)를 입력해 주세요.") @Size(max = 2000, message = "중단 이유는 2000자까지입니다.")
        String reason,
    @Schema(nullable = true) @Size(max = 2000, message = "결제원 조치사항은 2000자까지입니다.") String kftcAction,
    @NotBlank(message = "담당자 이름을 입력해 주세요.") @Size(max = 50, message = "담당자 이름은 50자까지입니다.")
        String contactName,
    @NotBlank(message = "담당자 연락처를 입력해 주세요.")
        @Pattern(regexp = "^[0-9][0-9-]{6,18}[0-9]$", message = "연락처는 숫자와 - 로 입력해 주세요.")
        String contactPhone,
    @Schema(nullable = true)
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자까지입니다.")
        String contactEmail,
    @Schema(nullable = true) @Size(max = 2000, message = "추가 사항은 2000자까지입니다.") String extraNote) {}
