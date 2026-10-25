package com.connectlink.organization;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.organization.dto.OrganizationOverviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "기관 한눈에 보기")
public class OrganizationOverviewController {

  private final OrganizationOverviewService overviewService;
  private final CurrentUserProvider currentUserProvider;

  public OrganizationOverviewController(
      OrganizationOverviewService overviewService, CurrentUserProvider currentUserProvider) {
    this.overviewService = overviewService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping("/{id}/overview")
  @Operation(
      summary = "기관 한눈에 보기",
      description =
          "참가기관 하나의 그 업무 담당자, 회신을 기다리는 의견 요청, 진행 중 기관 테스트, 진행 중·예정·최근 일시중단. 결제원 담당자만 본다(그 밖 403). 그 업무에 참여하지 않는 기관이면 404 ORGANIZATION_NOT_FOUND.")
  public OrganizationOverviewResponse overview(
      @PathVariable Long id,
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return overviewService.overview(
        currentUserProvider.require(),
        Business.fromSlug(business)
            .orElseThrow(
                () ->
                    BusinessException.notFound(
                        "BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + business)),
        id);
  }
}
