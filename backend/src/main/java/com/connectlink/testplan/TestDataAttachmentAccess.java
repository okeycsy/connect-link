package com.connectlink.testplan;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대상 기관별 테스트데이터 파일 첨부 권한(ownerId = 대상 기관 행 ID). 그 기관 결과를 볼 수 있으면 내려받고, 테스트데이터를 요청한 테스트에서 그 기관 담당자만
 * 올리고 지운다.
 */
@Component
@Transactional(readOnly = true)
class TestDataAttachmentAccess implements AttachmentOwnerAccess {

  private final TestPlanTargetRepository targetRepository;
  private final TestPlanAccessPolicy accessPolicy;

  TestDataAttachmentAccess(
      TestPlanTargetRepository targetRepository, TestPlanAccessPolicy accessPolicy) {
    this.targetRepository = targetRepository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.TEST_DATA;
  }

  @Override
  public boolean canView(CurrentUser user, Long ownerId) {
    return targetRepository
        .findById(ownerId)
        .map(t -> accessPolicy.canViewTarget(user, t))
        .orElse(false);
  }

  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return targetRepository
        .findById(ownerId)
        .map(t -> t.getPlan().isTestDataRequested() && accessPolicy.canReport(user, t))
        .orElse(false);
  }
}
