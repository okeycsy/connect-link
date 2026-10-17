package com.connectlink.opinion;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 의견 요청 모듈의 첨부 권한. 요청 첨부자료·업권별 회신 양식 파일·기관 회신 근거자료 세 대상이다. 보기는 대상을 볼 수 있는 사람, 올리기·지우기는 고칠 수 있는
 * 사람(요청·양식은 그 업무의 결제원 담당자가 마감 전까지, 근거자료는 그 기관 담당자가 회신을 고칠 수 있을 때)이다.
 */
final class OpinionAttachmentAccess {

  private OpinionAttachmentAccess() {}

  /** 요청 첨부자료. 요청을 볼 수 있으면 내려받는다. */
  @Component
  @Transactional(readOnly = true)
  static class RequestFiles implements AttachmentOwnerAccess {

    private final OpinionRequestRepository repository;
    private final OpinionAccessPolicy accessPolicy;

    RequestFiles(OpinionRequestRepository repository, OpinionAccessPolicy accessPolicy) {
      this.repository = repository;
      this.accessPolicy = accessPolicy;
    }

    @Override
    public AttachmentOwnerType ownerType() {
      return AttachmentOwnerType.OPINION_REQUEST;
    }

    @Override
    public boolean canView(CurrentUser user, Long ownerId) {
      return repository.findById(ownerId).map(r -> accessPolicy.canView(user, r)).orElse(false);
    }

    @Override
    public boolean canEdit(CurrentUser user, Long ownerId) {
      return repository
          .findById(ownerId)
          .map(r -> canEditRequest(accessPolicy, user, r))
          .orElse(false);
    }
  }

  /** 업권별 회신 양식 파일. 기관 담당자는 자기 기관에 고른 양식만 내려받는다. */
  @Component
  @Transactional(readOnly = true)
  static class FormFiles implements AttachmentOwnerAccess {

    private final OpinionReplyFormRepository repository;
    private final OpinionAccessPolicy accessPolicy;

    FormFiles(OpinionReplyFormRepository repository, OpinionAccessPolicy accessPolicy) {
      this.repository = repository;
      this.accessPolicy = accessPolicy;
    }

    @Override
    public AttachmentOwnerType ownerType() {
      return AttachmentOwnerType.OPINION_REPLY_FORM;
    }

    @Override
    public boolean canView(CurrentUser user, Long ownerId) {
      return repository
          .findById(ownerId)
          .map(
              form -> {
                OpinionRequest request = form.getRequest();
                if (accessPolicy.canManage(user, request.getBusiness())) {
                  return true;
                }
                return accessPolicy.isTargetMember(user, request)
                    && OpinionRequestService.ownTarget(user, request)
                        .map(t -> t.getForm() != null && t.getForm().getId().equals(form.getId()))
                        .orElse(false);
              })
          .orElse(false);
    }

    @Override
    public boolean canEdit(CurrentUser user, Long ownerId) {
      return repository
          .findById(ownerId)
          .map(form -> canEditRequest(accessPolicy, user, form.getRequest()))
          .orElse(false);
    }
  }

  /** 기관 회신 근거자료. 결제원 담당자와 그 기관의 같은 업무 담당자가 본다. 그 기관 담당자가 회신을 고칠 수 있을 때만 올린다. */
  @Component
  @Transactional(readOnly = true)
  static class ReplyFiles implements AttachmentOwnerAccess {

    private final OpinionReplyRepository repository;
    private final OpinionAccessPolicy accessPolicy;

    ReplyFiles(OpinionReplyRepository repository, OpinionAccessPolicy accessPolicy) {
      this.repository = repository;
      this.accessPolicy = accessPolicy;
    }

    @Override
    public AttachmentOwnerType ownerType() {
      return AttachmentOwnerType.OPINION_REPLY;
    }

    @Override
    public boolean canView(CurrentUser user, Long ownerId) {
      return repository
          .findById(ownerId)
          .map(r -> canViewReply(accessPolicy, user, r))
          .orElse(false);
    }

    @Override
    public boolean canEdit(CurrentUser user, Long ownerId) {
      return repository
          .findById(ownerId)
          .map(
              reply ->
                  isOwnOrganization(accessPolicy, user, reply)
                      && reply.getTarget().getRequest().getStatus() == OpinionRequestStatus.OPEN
                      && reply.isEditable())
          .orElse(false);
    }
  }

  private static boolean canEditRequest(
      OpinionAccessPolicy accessPolicy, CurrentUser user, OpinionRequest request) {
    return accessPolicy.canManage(user, request.getBusiness())
        && request.getStatus() != OpinionRequestStatus.CLOSED;
  }

  /** 회신을 볼 수 있는지: 그 업무의 결제원 담당자, 또는 회신한 기관의 같은 업무 담당자. */
  static boolean canViewReply(
      OpinionAccessPolicy accessPolicy, CurrentUser user, OpinionReply reply) {
    return accessPolicy.canManage(user, reply.getTarget().getRequest().getBusiness())
        || isOwnOrganization(accessPolicy, user, reply);
  }

  private static boolean isOwnOrganization(
      OpinionAccessPolicy accessPolicy, CurrentUser user, OpinionReply reply) {
    OpinionTarget target = reply.getTarget();
    return accessPolicy.isTargetMember(user, target.getRequest())
        && target.getOrganization().getCode().equals(user.organizationCode());
  }
}
