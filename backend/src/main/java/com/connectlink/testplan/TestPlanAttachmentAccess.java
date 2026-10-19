package com.connectlink.testplan;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 기관 테스트 가이드·참고자료 첨부 권한. 테스트를 볼 수 있으면(대상 기관 담당자 포함) 내려받고, 그 업무 결제원 담당자가 올리고 지운다. */
@Component
@Transactional(readOnly = true)
class TestPlanAttachmentAccess implements AttachmentOwnerAccess {

  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;

  TestPlanAttachmentAccess(TestPlanRepository repository, TestPlanAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.TEST_PLAN;
  }

  @Override
  public boolean canView(CurrentUser user, Long ownerId) {
    return repository.findById(ownerId).map(p -> accessPolicy.canView(user, p)).orElse(false);
  }

  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return repository
        .findById(ownerId)
        .map(p -> accessPolicy.canManage(user, p.getBusiness()))
        .orElse(false);
  }
}
