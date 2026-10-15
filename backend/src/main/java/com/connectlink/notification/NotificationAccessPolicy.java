package com.connectlink.notification;

import com.connectlink.auth.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** 알림 모듈의 권한 규칙. 알림은 받는 사람만, 발송함은 관리자만 본다. */
@Component
public class NotificationAccessPolicy {

  public boolean canRead(CurrentUser user, Notification notification) {
    return notification.getRecipientEmail().equalsIgnoreCase(user.email());
  }

  public void ensureCanRead(CurrentUser user, Notification notification) {
    if (!canRead(user, notification)) {
      throw new AccessDeniedException("다른 사람의 알림입니다.");
    }
  }

  public boolean canViewOutbox(CurrentUser user) {
    return user.admin();
  }

  public void ensureCanViewOutbox(CurrentUser user) {
    if (!canViewOutbox(user)) {
      throw new AccessDeniedException("발송함은 관리자만 볼 수 있습니다.");
    }
  }
}
