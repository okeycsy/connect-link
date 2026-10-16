package com.connectlink.account.demo;

import com.connectlink.account.AccountService;
import com.connectlink.account.demo.dto.DemoAccountResponse;
import com.connectlink.account.demo.dto.DemoAccountsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데모 도구: 로그인 화면과 계정 전환에서 고를 역할별 데모 계정. local·demo·test 프로필에만 있다. 데모 계정은 시드(db/seed)로만 생기며 비밀번호는
 * 공통값({@code app.demo.account-password}, README 에도 적혀 있다)이다. 화면은 이 값으로 실제 로그인 API 를 부른다.
 */
@RestController
@RequestMapping("/api/v1/demo/accounts")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class DemoAccountController {

  private final AccountService accountService;
  private final String accountPassword;

  public DemoAccountController(
      AccountService accountService,
      @Value("${app.demo.account-password}") String accountPassword) {
    this.accountService = accountService;
    this.accountPassword = accountPassword;
  }

  @GetMapping
  @Operation(summary = "데모 계정 목록 (역할별, 로그인 전에도 열림)")
  public DemoAccountsResponse list() {
    return new DemoAccountsResponse(
        accountPassword,
        accountService.listActive().stream().map(DemoAccountResponse::from).toList());
  }
}
