package com.connectlink.contact.dto;

import com.connectlink.contact.ContactRank;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 담당자 등록·수정 양식. 등록할 때 결제원 담당자는 기관을 고르고, 기관 담당자는 자기 기관으로 정해진다({@code organizationId} 를 무시한다). 수정할 때
 * 기관과 업무는 바뀌지 않는다(보내도 버린다).
 */
public record ContactSaveRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @Schema(nullable = true) Long organizationId,
    @Schema(nullable = true) @Size(max = 200, message = "세부업무는 200자까지 쓸 수 있습니다.")
        String subBusiness,
    @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50, message = "이름은 50자까지 쓸 수 있습니다.")
        String name,
    @NotBlank(message = "부서를 입력해 주세요.") @Size(max = 100, message = "부서는 100자까지 쓸 수 있습니다.")
        String department,
    @Schema(nullable = true) @Size(max = 100, message = "팀은 100자까지 쓸 수 있습니다.") String team,
    @Schema(nullable = true) @Size(max = 50, message = "직급은 50자까지 쓸 수 있습니다.") String jobTitle,
    @NotNull(message = "직무를 골라 주세요.") JobRole jobRole,
    @Schema(nullable = true, description = "전산 세부 직무. 직무가 전산이면 하나 이상, 현업이면 비워 둔다(보내도 버린다).")
        List<ItRole> itRoles,
    @NotNull(message = "주/부담당을 골라 주세요.") ContactRank rank,
    @NotBlank(message = "내선번호를 입력해 주세요.")
        @Pattern(regexp = "^$|^[0-9-]{2,30}$", message = "내선번호는 숫자와 - 로 2~30자입니다.")
        String extensionPhone,
    @Schema(nullable = true)
        @Pattern(
            regexp = "^$|^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
            message = "휴대폰번호는 010-0000-0000 꼴로 입력해 주세요.")
        String mobilePhone,
    @NotBlank(message = "회사 이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자까지 쓸 수 있습니다.")
        String email) {}
