package com.connectlink.history;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.PageResponse;
import com.connectlink.history.dto.ChangeHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/change-histories")
@Tag(name = "변경 이력", description = "대상(기관, 담당자, 의견 요청 등)별 변경 전·후 이력")
public class ChangeHistoryController {

  private final ChangeHistoryService changeHistoryService;

  public ChangeHistoryController(ChangeHistoryService changeHistoryService) {
    this.changeHistoryService = changeHistoryService;
  }

  @GetMapping
  @Operation(summary = "대상 하나의 변경 이력(최신순)")
  public PageResponse<ChangeHistoryResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam HistoryTargetType targetType,
      @RequestParam Long targetId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return changeHistoryService.list(
        user, targetType, targetId, PageRequest.of(page, Math.min(size, 100)));
  }
}
