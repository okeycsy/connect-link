package com.connectlink.notice;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.notice.dto.NoticeResponse;
import com.connectlink.notice.dto.NoticeSaveRequest;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notices")
@Tag(name = "공지·자료실", description = "업무별 공지·규약·시행세칙과 전 업무 공통 공지")
public class NoticeController {

  private final NoticeService noticeService;
  private final CurrentUserProvider currentUserProvider;

  public NoticeController(NoticeService noticeService, CurrentUserProvider currentUserProvider) {
    this.noticeService = noticeService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "공지·자료실 목록",
      description = "이 업무의 글과 전 업무 공통 공지. 고정 글이 먼저, 그다음 최근 글이다. 그 업무를 맡은 사람만 본다(그 밖 403).")
  public PageResponse<NoticeResponse> list(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @Parameter(description = "NOTICE(공지), RULE(규약), ENFORCEMENT_RULE(시행세칙). 없으면 전부")
          @RequestParam(required = false)
          NoticeCategory category,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return noticeService.list(
        currentUserProvider.require(),
        resolveBusiness(business),
        category,
        PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "공지·자료실 글 조회")
  public NoticeResponse get(@PathVariable Long id) {
    return noticeService.get(currentUserProvider.require(), id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "공지·자료실 글 올리기",
      description =
          "업무 글은 그 업무 결제원 담당자가, 업무를 비운 전 업무 공통 공지는 관리자가 올린다. 규약·시행세칙을 공통으로 올리면 400 NOTICE_COMMON_REGULATION.")
  public NoticeResponse create(@Valid @RequestBody NoticeSaveRequest request) {
    return noticeService.create(currentUserProvider.require(), request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "공지·자료실 글 고치기", description = "올릴 수 있는 사람이 고친다. 업무는 바꾸지 않는다.")
  public NoticeResponse update(
      @PathVariable Long id, @Valid @RequestBody NoticeSaveRequest request) {
    return noticeService.update(currentUserProvider.require(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "공지·자료실 글 지우기", description = "올릴 수 있는 사람이 지운다. 첨부도 함께 지운다.")
  public void delete(@PathVariable Long id) {
    noticeService.delete(currentUserProvider.require(), id);
  }

  private static Business resolveBusiness(String slug) {
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
