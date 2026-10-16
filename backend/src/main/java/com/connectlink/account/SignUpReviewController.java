package com.connectlink.account;

import com.connectlink.account.dto.SignUpApplicationCountsResponse;
import com.connectlink.account.dto.SignUpApplicationResponse;
import com.connectlink.account.dto.SignUpRejectionRequest;
import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리 > 가입 승인. 신청한 업무를 맡은 결제원 담당자와 관리자가 쓴다. 그 밖의 사용자는 403 이다. */
@RestController
@RequestMapping("/api/v1/sign-ups")
@Tag(name = "가입 승인", description = "가입 신청 목록과 승인·반려")
public class SignUpReviewController {

  private final SignUpReviewService reviewService;
  private final CurrentUserProvider currentUserProvider;

  public SignUpReviewController(
      SignUpReviewService reviewService, CurrentUserProvider currentUserProvider) {
    this.reviewService = reviewService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "가입 신청 목록 조회",
      description =
          "한 업무 영역을 신청에 넣은 계정을 상태별로 최근 신청 순서로 돌려준다. 승인(ACTIVE)·반려(REJECTED)는 가입 승인을 거친 계정만 나온다.")
  public PageResponse<SignUpApplicationResponse> list(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @RequestParam(defaultValue = "PENDING_APPROVAL") AccountStatus status,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return reviewService.list(
        currentUserProvider.require(),
        resolveBusiness(business),
        status,
        PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/counts")
  @Operation(summary = "가입 신청 건수 조회", description = "한 업무 영역의 승인 대기·승인·반려 건수. 목록의 필터 칩과 관리 화면이 쓴다.")
  public SignUpApplicationCountsResponse counts(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return reviewService.counts(currentUserProvider.require(), resolveBusiness(business));
  }

  @GetMapping("/{id}")
  @Operation(summary = "가입 신청 상세 조회", description = "신청한 업무 중 하나라도 맡은 결제원 담당자나 관리자만 본다.")
  public SignUpApplicationResponse get(@PathVariable Long id) {
    return reviewService.get(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/approval")
  @Operation(
      summary = "가입 승인",
      description = "계정을 사용 중으로 바꾸고 신청자에게 승인 알림과 메일을 보낸다. 승인 대기가 아니면 409 SIGN_UP_ALREADY_REVIEWED.")
  public SignUpApplicationResponse approve(@PathVariable Long id) {
    return reviewService.approve(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/rejection")
  @Operation(
      summary = "가입 반려",
      description = "사유를 받아 계정을 반려로 바꾸고 신청자에게 반려 메일을 보낸다. 반려된 이메일은 다시 가입 신청할 수 있다. 승인 대기가 아니면 409.")
  public SignUpApplicationResponse reject(
      @PathVariable Long id, @Valid @RequestBody SignUpRejectionRequest request) {
    return reviewService.reject(currentUserProvider.require(), id, request.reason());
  }

  private static Business resolveBusiness(String slug) {
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
