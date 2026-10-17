package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
import com.connectlink.history.HistoryTargetAccess;
import com.connectlink.history.HistoryTargetType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 의견 요청 모듈의 변경 이력 권한. 요청 이력은 그 업무의 결제원 담당자가, 회신 이력은 거기에 더해 회신한 기관의 담당자가 본다. */
final class OpinionHistoryAccess {

  private OpinionHistoryAccess() {}

  @Component
  @Transactional(readOnly = true)
  static class RequestHistory implements HistoryTargetAccess {

    private final OpinionRequestRepository repository;
    private final OpinionAccessPolicy accessPolicy;

    RequestHistory(OpinionRequestRepository repository, OpinionAccessPolicy accessPolicy) {
      this.repository = repository;
      this.accessPolicy = accessPolicy;
    }

    @Override
    public HistoryTargetType targetType() {
      return HistoryTargetType.OPINION_REQUEST;
    }

    @Override
    public boolean canView(CurrentUser user, Long targetId) {
      return repository
          .findById(targetId)
          .map(r -> accessPolicy.canManage(user, r.getBusiness()))
          .orElse(false);
    }
  }

  @Component
  @Transactional(readOnly = true)
  static class ReplyHistory implements HistoryTargetAccess {

    private final OpinionReplyRepository repository;
    private final OpinionAccessPolicy accessPolicy;

    ReplyHistory(OpinionReplyRepository repository, OpinionAccessPolicy accessPolicy) {
      this.repository = repository;
      this.accessPolicy = accessPolicy;
    }

    @Override
    public HistoryTargetType targetType() {
      return HistoryTargetType.OPINION_REPLY;
    }

    @Override
    public boolean canView(CurrentUser user, Long targetId) {
      return repository
          .findById(targetId)
          .map(r -> OpinionAttachmentAccess.canViewReply(accessPolicy, user, r))
          .orElse(false);
    }
  }
}
