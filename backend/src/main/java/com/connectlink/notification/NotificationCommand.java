package com.connectlink.notification;

import com.connectlink.organization.Business;
import java.util.Set;

/**
 * 알림 발행 요청. 기능 모듈은 이 레코드를 만들어 {@link NotificationService#publish} 에 넘긴다.
 *
 * @param recipientEmail 받는 사람의 회사 이메일
 * @param business 업무 영역. 업무와 무관하면 null
 * @param link 눌렀을 때 열 화면 주소. 없으면 null
 * @param channels 앱 안 알림에 더해 보낼 채널. 비어 있으면 알림 센터에만 남는다
 */
public record NotificationCommand(
    String recipientEmail,
    NotificationType type,
    Business business,
    String title,
    String body,
    String link,
    Set<NotificationChannel> channels) {

  public static NotificationCommand inAppOnly(
      String recipientEmail,
      NotificationType type,
      Business business,
      String title,
      String body,
      String link) {
    return new NotificationCommand(recipientEmail, type, business, title, body, link, Set.of());
  }
}
