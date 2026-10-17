package com.connectlink.opinion;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.opinion.dto.OpinionMyReplyResponse;
import com.connectlink.opinion.dto.OpinionReasonRequest;
import com.connectlink.opinion.dto.OpinionReplySaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기관 회신: 기관 담당자의 임시저장·제출·수정 요청과, 결제원 담당자의 수정 요청 승인·반려. */
@RestController
@RequestMapping("/api/v1/opinion-requests/{requestId}")
@Tag(name = "기관 회신", description = "참가기관의 의견 회신")
public class OpinionReplyController {

  private final OpinionReplyService replyService;
  private final CurrentUserProvider currentUserProvider;

  public OpinionReplyController(
      OpinionReplyService replyService, CurrentUserProvider currentUserProvider) {
    this.replyService = replyService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping("/my-reply")
  @Operation(
      summary = "내 기관 회신 조회",
      description =
          "제출한 내용과 임시저장한 내용을 함께 준다. 아직 저장한 적이 없으면 replyId·status 가 null 이다. 대상 기관 담당자만 본다.")
  public OpinionMyReplyResponse getMine(@PathVariable Long requestId) {
    return replyService.getMine(currentUserProvider.require(), requestId);
  }

  @PutMapping("/my-reply")
  @Operation(
      summary = "회신 임시저장",
      description = "처음 쓰는 중이거나 수정 요청이 승인된 뒤에만 저장한다. 제출한 내용은 바뀌지 않는다. 회신중 요청에만 된다.")
  public OpinionMyReplyResponse saveDraft(
      @PathVariable Long requestId, @Valid @RequestBody OpinionReplySaveRequest request) {
    return replyService.saveDraft(currentUserProvider.require(), requestId, request);
  }

  @PostMapping("/my-reply/submission")
  @Operation(summary = "회신 최종제출", description = "필수 질문에 모두 답해야 한다(400). 제출한 뒤 고치려면 수정 요청을 한다.")
  public OpinionMyReplyResponse submit(
      @PathVariable Long requestId, @Valid @RequestBody OpinionReplySaveRequest request) {
    return replyService.submit(currentUserProvider.require(), requestId, request);
  }

  @PostMapping("/my-reply/revision-request")
  @Operation(
      summary = "제출한 회신 수정 요청",
      description = "사유를 적어 요청한다. 요청을 만든 결제원 담당자에게 알림이 가고, 승인하면 고쳐서 다시 제출한다.")
  public OpinionMyReplyResponse requestRevision(
      @PathVariable Long requestId, @Valid @RequestBody OpinionReasonRequest request) {
    return replyService.requestRevision(currentUserProvider.require(), requestId, request.reason());
  }

  @PostMapping("/replies/{replyId}/revision-approval")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "회신 수정 요청 승인", description = "그 업무의 결제원 담당자가 승인한다. 기관 담당자에게 알림이 간다.")
  public void approveRevision(@PathVariable Long requestId, @PathVariable Long replyId) {
    replyService.approveRevision(currentUserProvider.require(), requestId, replyId);
  }

  @PostMapping("/replies/{replyId}/revision-rejection")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "회신 수정 요청 반려", description = "사유를 적어 받아들이지 않는다. 회신은 제출 상태로 돌아간다.")
  public void rejectRevision(
      @PathVariable Long requestId,
      @PathVariable Long replyId,
      @Valid @RequestBody OpinionReasonRequest request) {
    replyService.rejectRevision(
        currentUserProvider.require(), requestId, replyId, request.reason());
  }
}
