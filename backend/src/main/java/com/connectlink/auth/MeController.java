package com.connectlink.auth;

import com.connectlink.auth.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자 정보. 상단 바의 사용자 표시와 메뉴 노출에 쓴다. */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "계정", description = "로그인 사용자")
public class MeController {

  private final CurrentUserProvider currentUserProvider;

  public MeController(CurrentUserProvider currentUserProvider) {
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(summary = "내 정보 조회", description = "로그인 사용자의 이름, 소속 기관, 관리자 여부를 돌려준다.")
  public CurrentUserResponse me() {
    return CurrentUserResponse.from(currentUserProvider.require());
  }
}
