package com.connectlink.search;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.organization.Business;
import com.connectlink.search.dto.SearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 전역 검색(Ctrl K) API. */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "검색", description = "전역 검색")
public class SearchController {

  private final SearchService searchService;
  private final CurrentUserProvider currentUserProvider;

  public SearchController(SearchService searchService, CurrentUserProvider currentUserProvider) {
    this.searchService = searchService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "전역 검색",
      description = "레일에서 고른 업무 영역 안에서 기관·담당자·의견 요청·기관 테스트·일시중단을 묶음별로 최대 5건씩 돌려준다. 빈 검색어면 묶음이 없다.")
  public SearchResponse search(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @Parameter(description = "검색어") @RequestParam(defaultValue = "") String q) {
    Business resolved =
        Business.fromSlug(business)
            .orElseThrow(
                () ->
                    BusinessException.notFound(
                        "BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + business));
    return searchService.search(currentUserProvider.require(), resolved, q);
  }
}
