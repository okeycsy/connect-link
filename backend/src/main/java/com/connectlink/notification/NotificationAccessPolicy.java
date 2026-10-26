package com.connectlink.notification;

import com.connectlink.auth.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 알림 모듈의 권한 규칙. 알림은 받는 사람만, 발송함은 관리자만 본다. 시연(local·demo)에서는 발송함이 모든 계정의 데모 도구 메뉴에 있으므로 {@code
 * app.demo.outbox-open-to-all} 을 켜 로그인한 누구나 본다.
 */
@Component
public class NotificationAccessPolicy {

  private final boolean outboxOpenToAll;

  public NotificationAccessPolicy(
      @Value("${app.demo.outbox-open-to-all:false}") boolean outboxOpenToAll) {
    this.outboxOpenToAll = outboxOpenToAll;
  }

  public boolean canRead(CurrentUser user, Notification notification) {
    return notification.getRecipientEmail().equalsIgnoreCase(user.email());
  }

  public void ensureCanRead(CurrentUser user, Notification notification) {
    if (!canRead(user, notification)) {
      throw new AccessDeniedException("다른 사람의 알림입니다.");
    }
  }

  public boolean canViewOutbox(CurrentUser user) {
    return outboxOpenToAll || user.admin();
  }

  public void ensureCanViewOutbox(CurrentUser user) {
    if (!canViewOutbox(user)) {
      throw new AccessDeniedException("발송함은 관리자만 볼 수 있습니다.");
    }
  }
}
