package com.connectlink.suspension;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 일시중단 공문 첨부 권한. 일시중단을 볼 수 있으면 내려받고, 고칠 수 있으면 올리고 지운다. */
@Component
@Transactional(readOnly = true)
class SuspensionAttachmentAccess implements AttachmentOwnerAccess {

  private final SuspensionRepository repository;
  private final SuspensionAccessPolicy accessPolicy;

  SuspensionAttachmentAccess(SuspensionRepository repository, SuspensionAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.SUSPENSION;
  }

  @Override
  public boolean canView(CurrentUser user, Long ownerId) {
    return repository.findById(ownerId).map(s -> accessPolicy.canView(user, s)).orElse(false);
  }

  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return repository.findById(ownerId).map(s -> accessPolicy.canEdit(user, s)).orElse(false);
  }
}
