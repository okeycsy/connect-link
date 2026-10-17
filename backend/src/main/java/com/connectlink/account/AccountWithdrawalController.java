package com.connectlink.account;

import com.connectlink.account.dto.AccountWithdrawalRequest;
import com.connectlink.account.dto.WithdrawalBlockerResponse;
import com.connectlink.account.dto.WithdrawalCheckResponse;
import com.connectlink.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 관리 > 가입 승인의 탈퇴 처리. 참가기관 담당자의 업무를 맡은 결제원 담당자와 관리자가 쓴다. 그 밖의 사용자는 403 이다. */
@RestController
@RequestMapping("/api/v1/accounts/{id}/withdrawal")
@Tag(name = "가입 승인", description = "가입 신청 목록과 승인·반려")
public class AccountWithdrawalController {

  private final WithdrawalService withdrawalService;
  private final CurrentUserProvider currentUserProvider;

  public AccountWithdrawalController(
      WithdrawalService withdrawalService, CurrentUserProvider currentUserProvider) {
    this.withdrawalService = withdrawalService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "탈퇴 처리 가능 여부 조회",
      description =
          "이 계정을 지금 탈퇴 처리할 수 있는지와, 막혀 있으면 그 까닭을 돌려준다. 사용 중 계정이 아니면 409 ACCOUNT_NOT_ACTIVE.")
  public WithdrawalCheckResponse check(@PathVariable Long id) {
    return WithdrawalCheckResponse.of(
        withdrawalService.blockersFor(currentUserProvider.require(), id).stream()
            .map(WithdrawalBlockerResponse::from)
            .toList());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "탈퇴 처리",
      description =
          "사유를 받아 참가기관 담당자의 계정을 탈퇴시키고, 그 담당자에게 사유를 담은 메일을 보낸다. 그 담당자의 세션은 다음 요청에서 끝난다. 사용 중 계정이"
              + " 아니면 409 ACCOUNT_NOT_ACTIVE, 막는 까닭이 있으면 409 WITHDRAWAL_BLOCKED.")
  public void withdraw(
      @PathVariable Long id, @Valid @RequestBody AccountWithdrawalRequest request) {
    withdrawalService.withdrawByStaff(currentUserProvider.require(), id, request.reason());
  }
}
