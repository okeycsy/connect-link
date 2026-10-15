package com.connectlink.attachment;

import com.connectlink.auth.CurrentUser;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** 첨부파일 모듈의 권한 규칙. 대상 종류별 {@link AttachmentOwnerAccess} 빈에 묻고, 빈이 없는 대상은 거부한다. */
@Component
public class AttachmentAccessPolicy {

  private final Map<AttachmentOwnerType, AttachmentOwnerAccess> accesses =
      new EnumMap<>(AttachmentOwnerType.class);

  public AttachmentAccessPolicy(List<AttachmentOwnerAccess> accessBeans) {
    for (AttachmentOwnerAccess access : accessBeans) {
      AttachmentOwnerAccess previous = accesses.putIfAbsent(access.ownerType(), access);
      if (previous != null) {
        throw new IllegalStateException(
            "첨부 대상 "
                + access.ownerType()
                + " 의 권한 빈이 둘입니다: "
                + previous.getClass()
                + ", "
                + access.getClass());
      }
    }
  }

  public boolean canView(CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    AttachmentOwnerAccess access = accesses.get(ownerType);
    return access != null && access.canView(user, ownerId);
  }

  public boolean canEdit(CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    AttachmentOwnerAccess access = accesses.get(ownerType);
    return access != null && access.canEdit(user, ownerId);
  }

  public void ensureCanView(CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    if (!canView(user, ownerType, ownerId)) {
      throw new AccessDeniedException("이 첨부파일을 볼 권한이 없습니다.");
    }
  }

  public void ensureCanEdit(CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    if (!canEdit(user, ownerType, ownerId)) {
      throw new AccessDeniedException("이 대상에 첨부파일을 올리거나 지울 권한이 없습니다.");
    }
  }
}
