package com.connectlink.account.dto;

import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 가입 신청. 화면의 4단계(① 회사 이메일 인증 ② 기관과 업무 ③ 직무 ④ 담당자 정보와 비밀번호)를 한 번에 보낸다. 서버가 모든 칸을 다시 검증하고, 업무가 기관의 참여
 * 업무인지와 전산 세부 직무 같은 규칙은 Service 가 본다.
 */
public record SignUpRequest(
    @NotBlank(message = "이메일 인증을 먼저 해 주세요.") String verificationToken,
    @NotBlank(message = "회사 이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 200, message = "이메일은 200자까지 쓸 수 있습니다.")
        String email,
    @NotNull(message = "기관을 골라 주세요.") Long organizationId,
    @NotEmpty(message = "담당 업무를 하나 이상 골라 주세요.") List<@Valid SignUpBusinessRequest> businesses,
    @NotNull(message = "직무를 골라 주세요.") JobRole jobRole,
    @Schema(nullable = true, description = "전산 세부 직무. 직무가 전산이면 하나 이상, 현업이면 비워 둔다(보내도 버린다).")
        List<ItRole> itRoles,
    @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50, message = "이름은 50자까지 쓸 수 있습니다.")
        String name,
    @NotBlank(message = "부서를 입력해 주세요.") @Size(max = 100, message = "부서는 100자까지 쓸 수 있습니다.")
        String department,
    @NotBlank(message = "팀을 입력해 주세요.") @Size(max = 100, message = "팀은 100자까지 쓸 수 있습니다.")
        String team,
    @NotBlank(message = "직급을 입력해 주세요.") @Size(max = 50, message = "직급은 50자까지 쓸 수 있습니다.")
        String jobTitle,
    @NotBlank(message = "내선번호를 입력해 주세요.")
        @Pattern(regexp = "^$|^[0-9-]{2,30}$", message = "내선번호는 숫자와 - 로 2~30자입니다.")
        String extensionPhone,
    @Schema(nullable = true)
        @Pattern(
            regexp = "^$|^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
            message = "휴대폰번호는 010-0000-0000 꼴로 입력해 주세요.")
        String mobilePhone,
    @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(regexp = SignUpRequest.PASSWORD_RULE, message = SignUpRequest.PASSWORD_MESSAGE)
        String password) {

  /** 영문, 숫자, 특수문자를 하나 이상씩 넣은 8~64자. 빈 값은 @NotBlank 가 따로 알린다. */
  public static final String PASSWORD_RULE =
      "^$|^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{8,64}$";

  public static final String PASSWORD_MESSAGE = "비밀번호는 영문, 숫자, 특수문자를 모두 넣어 8~64자로 정해 주세요.";
}
