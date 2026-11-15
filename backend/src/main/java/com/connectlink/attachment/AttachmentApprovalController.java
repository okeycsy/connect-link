package com.connectlink.attachment;

import com.connectlink.attachment.dto.AttachmentApprovalBatchResponse;
import com.connectlink.attachment.dto.AttachmentRejectionRequest;
import com.connectlink.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 파일 반출 결재. 결재는 등록 한 건(첨부 대상 하나)에 붙인 파일 단위로, 팀장이 알림 센터에서 연다. */
@RestController
@RequestMapping("/api/v1/attachment-approvals/{ownerType}/{ownerId}")
@Tag(name = "파일 반출 결재", description = "결제원 담당자가 올린 파일을 같은 팀 팀장이 승인해야 참가기관에 보인다")
public class AttachmentApprovalController {

  private final AttachmentApprovalService approvalService;

  public AttachmentApprovalController(AttachmentApprovalService approvalService) {
    this.approvalService = approvalService;
  }

  @GetMapping
  @Operation(summary = "결재 한 건 보기 (결재자)", description = "그 대상에서 내가 결재자인 첨부파일과 결재 상태.")
  public AttachmentApprovalBatchResponse get(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable AttachmentOwnerType ownerType,
      @PathVariable Long ownerId) {
    return approvalService.get(user, ownerType, ownerId);
  }

  @PostMapping("/approval")
  @Operation(summary = "첨부파일 반출 승인 (결재자, 내부망)", description = "결재 대기 파일을 모두 승인한다.")
  public AttachmentApprovalBatchResponse approve(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable AttachmentOwnerType ownerType,
      @PathVariable Long ownerId) {
    return approvalService.approve(user, ownerType, ownerId);
  }

  @PostMapping("/rejection")
  @Operation(summary = "첨부파일 반출 반려 (결재자, 내부망, 사유 필수)", description = "결재 대기 파일을 모두 반려한다.")
  public AttachmentApprovalBatchResponse reject(
      @AuthenticationPrincipal CurrentUser user,
      @PathVariable AttachmentOwnerType ownerType,
      @PathVariable Long ownerId,
      @Valid @RequestBody AttachmentRejectionRequest request) {
    return approvalService.reject(user, ownerType, ownerId, request.reason());
  }
}
