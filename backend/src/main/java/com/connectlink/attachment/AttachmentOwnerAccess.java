package com.connectlink.attachment;

import com.connectlink.auth.CurrentUser;

/**
 * 첨부 대상별 권한. 기능 모듈이 자기 {@link AttachmentOwnerType} 에 대해 이 인터페이스의 빈을 하나 만든다. 첨부는 대상을 볼 수 있는 사람만
 * 내려받고, 대상을 고칠 수 있는 사람만 올리거나 지운다. 빈이 없는 대상은 아무도 쓸 수 없다.
 */
public interface AttachmentOwnerAccess {

  AttachmentOwnerType ownerType();

  /** 대상의 첨부 목록을 보고 내려받을 수 있는가. */
  boolean canView(CurrentUser user, Long ownerId);

  /** 대상에 첨부를 올리고 지울 수 있는가. */
  boolean canEdit(CurrentUser user, Long ownerId);
}
