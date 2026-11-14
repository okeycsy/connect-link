package com.connectlink.attachment;

import com.connectlink.auth.CurrentUser;
import java.util.Optional;

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

  /**
   * 첨부가 하나 이상 꼭 있어야 하는 대상이면 마지막 첨부를 지우려 할 때 보일 문구(예: 일시중단 공문). 비어 있으면 마지막 첨부도 지울 수 있다. 대상 자체를
   * 지울 때({@link AttachmentService#deleteAllOf})는 따지지 않는다.
   */
  default Optional<String> requiredMessage(Long ownerId) {
    return Optional.empty();
  }
}
