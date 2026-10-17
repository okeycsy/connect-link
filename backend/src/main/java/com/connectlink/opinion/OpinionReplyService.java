package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.opinion.dto.OpinionMyReplyResponse;
import com.connectlink.opinion.dto.OpinionReplySaveRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 회신. 기관 담당자의 임시저장·최종제출·제출 내용 확인·수정 요청과, 결제원 담당자의 수정 요청 승인·반려를 맡는다. 회신은 대상 기관 하나에 하나이며 그 기관의 같은
 * 업무 담당자 누구나 이어서 쓴다. 회신중 요청에만 저장·제출·수정 요청을 받는다.
 */
@Service
@Transactional
public class OpinionReplyService {

  private final OpinionRequestService requestService;
  private final OpinionReplyRepository replyRepository;
  private final OpinionAccessPolicy accessPolicy;
  private final ChangeHistoryService changeHistoryService;
  private final OpinionNotifier notifier;

  public OpinionReplyService(
      OpinionRequestService requestService,
      OpinionReplyRepository replyRepository,
      OpinionAccessPolicy accessPolicy,
      ChangeHistoryService changeHistoryService,
      OpinionNotifier notifier) {
    this.requestService = requestService;
    this.replyRepository = replyRepository;
    this.accessPolicy = accessPolicy;
    this.changeHistoryService = changeHistoryService;
    this.notifier = notifier;
  }

  /** 내 기관의 회신. 아직 저장한 적이 없으면 빈 회신(상태 null)이다. */
  @Transactional(readOnly = true)
  public OpinionMyReplyResponse getMine(CurrentUser user, Long requestId) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    return toResponse(
        request, target, replyRepository.findByTarget_Id(target.getId()).orElse(null));
  }

  /** 임시저장. 처음 쓰는 중이거나 수정 요청이 승인된 뒤에만 된다. 제출한 내용은 그대로 둔다. */
  public OpinionMyReplyResponse saveDraft(
      CurrentUser user, Long requestId, OpinionReplySaveRequest body) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    ensureOpen(request);
    OpinionReply reply = findOrStart(target);
    ensureEditable(reply);
    reply.saveDraft(body.type(), trimToNull(body.content()), user.email(), user.name());
    replyRepository.saveAndFlush(reply);
    return toResponse(request, target, reply);
  }

  /** 최종제출. 의견 유형이 있어야 하고, 조건부 동의·이견은 세부의견도 있어야 한다. 처음 제출이면 등록 이력을, 수정 뒤 다시 제출이면 바뀐 항목을 이력으로 남긴다. */
  public OpinionMyReplyResponse submit(
      CurrentUser user, Long requestId, OpinionReplySaveRequest body) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    ensureOpen(request);
    if (body.type() == null) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "OPINION_REPLY_TYPE_REQUIRED", "type", "의견 유형을 골라 주세요.");
    }
    String content = trimToNull(body.content());
    if (body.type() != OpinionReplyType.AGREE && content == null) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "OPINION_REPLY_CONTENT_REQUIRED",
          "content",
          body.type().getLabel() + "은(는) 세부의견을 적어 주세요.");
    }
    OpinionReply reply = findOrStart(target);
    ensureEditable(reply);
    boolean resubmit = reply.isReplied();
    OpinionReplyType beforeType = reply.getType();
    String beforeContent = reply.getContent();
    reply.submit(body.type(), content, user.email(), user.name(), requestService.now());
    replyRepository.saveAndFlush(reply);
    String organization = target.getOrganization().getName();
    changeHistoryService.record(
        user,
        resubmit
            ? ChangeHistoryCommand.updated(
                HistoryTargetType.OPINION_REPLY,
                reply.getId(),
                request.getBusiness(),
                organization + " 회신 수정 제출",
                FieldChanges.compare()
                    .field("type", "의견 유형", beforeType, reply.getType())
                    .field("content", "세부의견", beforeContent, reply.getContent())
                    .toList())
            : ChangeHistoryCommand.created(
                HistoryTargetType.OPINION_REPLY,
                reply.getId(),
                request.getBusiness(),
                organization + " 회신 제출",
                FieldChanges.compare()
                    .value("type", "의견 유형", null, reply.getType())
                    .value("content", "세부의견", null, reply.getContent())
                    .toList()));
    return toResponse(request, target, reply);
  }

  /** 제출한 회신을 고치고 싶다고 요청한다. 요청을 만든 결제원 담당자가 승인하면 고칠 수 있다. */
  public OpinionMyReplyResponse requestRevision(CurrentUser user, Long requestId, String reason) {
    OpinionRequest request = requestService.load(requestId);
    OpinionTarget target = ownTarget(user, request);
    ensureOpen(request);
    OpinionReply reply =
        replyRepository
            .findByTarget_Id(target.getId())
            .filter(r -> r.getStatus() == OpinionReplyStatus.SUBMITTED)
            .orElseThrow(
                () -> BusinessException.conflict("OPINION_REPLY_STATUS", "제출한 회신만 수정 요청할 수 있습니다."));
    reply.requestRevision(reason.trim(), user.email(), user.name(), requestService.now());
    notifier.revisionRequested(request, reply);
    return toResponse(request, target, reply);
  }

  /** 수정 요청을 승인한다. 기관은 제출한 내용에서 고쳐 다시 제출한다. */
  public void approveRevision(CurrentUser user, Long requestId, Long replyId) {
    OpinionRequest request = requestService.loadManaged(user, requestId);
    ensureOpen(request);
    OpinionReply reply = loadRevisionRequested(request, replyId);
    reply.approveRevision();
    notifier.revisionReviewed(request, reply, true);
  }

  /** 수정 요청을 받아들이지 않는다. 제출 상태로 돌아가고 사유가 기관 담당자에게 간다. */
  public void rejectRevision(CurrentUser user, Long requestId, Long replyId, String reason) {
    OpinionRequest request = requestService.loadManaged(user, requestId);
    ensureOpen(request);
    OpinionReply reply = loadRevisionRequested(request, replyId);
    reply.rejectRevision(reason.trim());
    notifier.revisionReviewed(request, reply, false);
  }

  private OpinionReply loadRevisionRequested(OpinionRequest request, Long replyId) {
    OpinionReply reply =
        replyRepository
            .findById(replyId)
            .filter(r -> r.getTarget().getRequest().getId().equals(request.getId()))
            .orElseThrow(
                () -> BusinessException.notFound("OPINION_REPLY_NOT_FOUND", "기관 회신을 찾을 수 없습니다."));
    if (reply.getStatus() != OpinionReplyStatus.REVISION_REQUESTED) {
      throw BusinessException.conflict("OPINION_REPLY_STATUS", "수정 요청한 회신이 아닙니다.");
    }
    return reply;
  }

  private OpinionTarget ownTarget(CurrentUser user, OpinionRequest request) {
    accessPolicy.ensureTargetMember(user, request);
    return OpinionRequestService.ownTarget(user, request).orElseThrow();
  }

  private OpinionReply findOrStart(OpinionTarget target) {
    return replyRepository
        .findByTarget_Id(target.getId())
        .orElseGet(() -> new OpinionReply(target));
  }

  private static void ensureOpen(OpinionRequest request) {
    if (request.getStatus() != OpinionRequestStatus.OPEN) {
      throw BusinessException.conflict(
          "OPINION_REQUEST_STATUS",
          request.isCancelled() ? "취소된 의견 요청입니다." : "마감한 의견 요청이라 회신을 받지 않습니다.");
    }
  }

  private static void ensureEditable(OpinionReply reply) {
    if (!reply.isEditable()) {
      throw BusinessException.conflict(
          "OPINION_REPLY_NOT_EDITABLE",
          reply.getStatus() == OpinionReplyStatus.REVISION_REQUESTED
              ? "수정 요청을 결제원 담당자가 승인하면 고칠 수 있습니다."
              : "이미 제출한 회신입니다. 고치려면 수정 요청을 해 주세요.");
    }
  }

  static OpinionMyReplyResponse toResponse(
      OpinionRequest request, OpinionTarget target, OpinionReply reply) {
    OpinionReplyForm form = target.getForm();
    boolean open = request.getStatus() == OpinionRequestStatus.OPEN;
    if (reply == null) {
      return new OpinionMyReplyResponse(
          request.getId(),
          target.getId(),
          target.getOrganization().getName(),
          form == null ? null : OpinionRequestService.toFormResponse(form),
          null,
          null,
          "미작성",
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          open,
          false);
    }
    return new OpinionMyReplyResponse(
        request.getId(),
        target.getId(),
        target.getOrganization().getName(),
        form == null ? null : OpinionRequestService.toFormResponse(form),
        reply.getId(),
        reply.getStatus(),
        reply.getStatus().getLabel(),
        reply.getType(),
        reply.getType() == null ? null : reply.getType().getLabel(),
        reply.getContent(),
        reply.getDraftType(),
        reply.getDraftContent(),
        reply.getReplierName(),
        reply.getSubmittedAt(),
        reply.getRevisionReason(),
        reply.getRevisionRequestedAt(),
        reply.getRevisionRejectionReason(),
        open && reply.isEditable(),
        open && reply.getStatus() == OpinionReplyStatus.SUBMITTED);
  }

  private static String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
