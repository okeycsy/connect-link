package com.connectlink.account;

import com.connectlink.account.dto.WithdrawalBlockerResponse;
import com.connectlink.account.dto.WithdrawalCheckResponse;
import com.connectlink.account.dto.WithdrawalRequest;
import com.connectlink.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 내 정보 > 탈퇴. 로그인한 본인만 자기 계정을 탈퇴시킨다. */
@RestController
@RequestMapping("/api/v1/me/withdrawal")
@Tag(name = "계정", description = "로그인 사용자")
public class WithdrawalController {

  private final WithdrawalService withdrawalService;
  private final CurrentUserProvider currentUserProvider;

  public WithdrawalController(
      WithdrawalService withdrawalService, CurrentUserProvider currentUserProvider) {
    this.withdrawalService = withdrawalService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "탈퇴 가능 여부 조회",
      description = "지금 탈퇴할 수 있는지와, 막혀 있으면 그 까닭(무엇을 하면 되는지, 그 화면 주소)을 모두 돌려준다.")
  public WithdrawalCheckResponse check() {
    return WithdrawalCheckResponse.of(
        withdrawalService.blockers(currentUserProvider.require()).stream()
            .map(WithdrawalBlockerResponse::from)
            .toList());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "탈퇴",
      description =
          "비밀번호를 다시 확인하고 계정을 탈퇴시킨 뒤 세션을 끝낸다. 계정 정보와 알림은 지우고 탈퇴 확인 메일을 보낸다. 비밀번호가 틀리면 400"
              + " PASSWORD_MISMATCH, 막는 까닭이 있으면 409 WITHDRAWAL_BLOCKED.")
  public void withdraw(
      @Valid @RequestBody WithdrawalRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    withdrawalService.withdraw(currentUserProvider.require(), request.password());
    new SecurityContextLogoutHandler()
        .logout(httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());
  }
}
