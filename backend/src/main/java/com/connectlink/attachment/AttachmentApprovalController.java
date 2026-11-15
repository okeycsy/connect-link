package com.connectlink.attachment;

import com.connectlink.attachment.dto.AttachmentApprovalCountResponse;
import com.connectlink.attachment.dto.AttachmentApprovalResponse;
import com.connectlink.attachment.dto.AttachmentRejectionRequest;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attachment-approvals")
@Tag(name = "파일 반출 결재", description = "결제원 담당자가 올린 파일을 같은 팀 팀장이 승인해야 참가기관에 보인다")
public class AttachmentApprovalController {

  private final AttachmentApprovalService approvalService;

  public AttachmentApprovalController(AttachmentApprovalService approvalService) {
    this.approvalService = approvalService;
  }

  @GetMapping
  @Operation(
      summary = "파일 결재함 목록",
      description =
          "box=TO_APPROVE 는 내가 결재할 파일, REQUESTED 는 내가 올린 파일. status 가 없으면 대기·승인·반려 모두(최신순).")
  public PageResponse<AttachmentApprovalResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "TO_APPROVE") AttachmentApprovalBox box,
      @RequestParam(required = false) AttachmentApprovalStatus status,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return approvalService.list(user, box, status, PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/pending-count")
  @Operation(summary = "내가 결재할 대기 파일 수")
  public AttachmentApprovalCountResponse pendingCount(@AuthenticationPrincipal CurrentUser user) {
    return new AttachmentApprovalCountResponse(approvalService.pendingCount(user));
  }

  @PostMapping("/{id}/approval")
  @Operation(summary = "파일 반출 승인 (결재자, 내부망)")
  public AttachmentApprovalResponse approve(
      @AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
    return approvalService.approve(user, id);
  }

  @PostMapping("/{id}/rejection")
  @Operation(summary = "파일 반출 반려 (결재자, 내부망, 사유 필수)")
  public AttachmentApprovalResponse reject(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable Long id,
      @Valid @RequestBody AttachmentRejectionRequest request) {
    return approvalService.reject(user, id, request.reason());
  }
}
