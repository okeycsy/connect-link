package com.connectlink.home;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.home.dto.HomeResponse;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 홈 "지금 할 일" API. */
@RestController
@RequestMapping("/api/v1/home")
@Tag(name = "홈", description = "지금 할 일 대시보드")
public class HomeController {

  private final HomeService homeService;
  private final CurrentUserProvider currentUserProvider;

  public HomeController(HomeService homeService, CurrentUserProvider currentUserProvider) {
    this.homeService = homeService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(summary = "홈 대시보드 조회", description = "레일에서 고른 업무 영역의 KPI 와 할 일 카드를 돌려준다.")
  public HomeResponse home(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    Business resolved =
        Business.fromSlug(business)
            .orElseThrow(
                () ->
                    BusinessException.notFound(
                        "BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + business));
    return homeService.summary(currentUserProvider.require(), resolved);
  }
}
