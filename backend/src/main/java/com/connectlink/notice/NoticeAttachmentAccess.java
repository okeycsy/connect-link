package com.connectlink.notice;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 공지·자료실 첨부(규약·시행세칙 파일 등) 권한. 글을 볼 수 있으면 내려받고, 글을 고칠 수 있으면 올리고 지운다. */
@Component
@Transactional(readOnly = true)
class NoticeAttachmentAccess implements AttachmentOwnerAccess {

  private final NoticeRepository repository;
  private final NoticeAccessPolicy accessPolicy;

  NoticeAttachmentAccess(NoticeRepository repository, NoticeAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.NOTICE;
  }

  @Override
  public boolean canView(CurrentUser user, Long ownerId) {
    return repository.findById(ownerId).map(n -> accessPolicy.canView(user, n)).orElse(false);
  }

  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return repository
        .findById(ownerId)
        .map(n -> accessPolicy.canManage(user, n.getBusiness()))
        .orElse(false);
  }
}
