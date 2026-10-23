package com.connectlink.suspension.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 대상 업무 기준정보 등록·수정 양식(관리자).
 *
 * @param businesses 관련 레일 업무. 비울 수 있다
 * @param active 끄면 새로 고를 수 없다
 */
public record TargetWorkSaveRequest(
    @NotBlank(message = "대상 업무 이름을 입력해 주세요.") @Size(max = 100, message = "이름은 100자까지입니다.")
        String name,
    @Schema(nullable = true) @Size(max = 50, message = "분류는 50자까지입니다.") String category,
    @NotNull(message = "순서를 입력해 주세요.") @Min(value = 0, message = "순서는 0 이상입니다.") Integer sortOrder,
    boolean active,
    @NotNull(message = "관련 업무를 보내 주세요.") List<@NotNull Business> businesses) {}
