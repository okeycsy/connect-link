package com.connectlink.testplan;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 테스트데이터 회신 양식 첨부 권한(ownerId = 테스트 ID). 테스트를 볼 수 있으면(대상 기관 담당자 포함) 내려받고, 그 업무 결제원 담당자가 올리고 지운다. */
@Component
@Transactional(readOnly = true)
class TestDataFormAttachmentAccess implements AttachmentOwnerAccess {

  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;

  TestDataFormAttachmentAccess(TestPlanRepository repository, TestPlanAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.TEST_DATA_FORM;
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
