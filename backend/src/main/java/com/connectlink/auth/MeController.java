package com.connectlink.auth;

import com.connectlink.auth.dto.CurrentUserResponse;
import com.connectlink.common.NetworkZoneResolver;
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
  private final NetworkZoneResolver networkZoneResolver;

  public MeController(
      CurrentUserProvider currentUserProvider, NetworkZoneResolver networkZoneResolver) {
    this.currentUserProvider = currentUserProvider;
    this.networkZoneResolver = networkZoneResolver;
  }

  @GetMapping
  @Operation(
      summary = "내 정보 조회",
      description = "로그인 사용자의 이름, 소속 기관, 관리자 여부와 지금 접속한 망(내부망/인터넷망)을 돌려준다.")
  public CurrentUserResponse me() {
    return CurrentUserResponse.from(currentUserProvider.require(), networkZoneResolver.current());
  }
}
