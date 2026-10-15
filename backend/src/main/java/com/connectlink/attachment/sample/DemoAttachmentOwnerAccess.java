package com.connectlink.attachment.sample;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 데모 도구의 첨부파일 화면용 권한. 로그인한 누구나 보고 올린다. 운영 프로필에는 없으므로 DEMO 대상은 운영에서 쓸 수 없다. */
@Component
@Profile({"local", "demo", "test"})
public class DemoAttachmentOwnerAccess implements AttachmentOwnerAccess {

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.DEMO;
  }

  @Override
  public boolean canView(CurrentUser user, Long ownerId) {
    return true;
  }

  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return true;
  }
}
