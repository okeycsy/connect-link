package com.connectlink.suspension;

import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 일시중단 모듈의 알림. 모두 {@link NotificationService} 를 거쳐 알림 센터에 남고, 채널은 명세를 따른다: 새로 등록하면 그 업무 결제원 담당자에게
 * 메일과 매터모스트(그 순서), 고치거나 지우면 매터모스트. 등록한 기관 담당자에게는 확정·수정 소식을 알림 센터로만 알린다.
 */
@Component
class SuspensionNotifier {

  private static final int TITLE_MAX = 200;
  private static final int BODY_MAX = 1000;
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final NotificationService notificationService;

  SuspensionNotifier(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /** 일시중단 화면 주소. */
  static String linkOf(Suspension suspension) {
    return "/" + suspension.getBusiness().getSlug() + "/suspensions/" + suspension.getId();
  }

  /** "2026-10-18 02:00~04:00" 꼴. 날짜가 다르면 끝에도 날짜를 붙인다. */
  static String periodText(LocalDateTime start, LocalDateTime end) {
    String head = start.format(DATE_TIME);
    String tail =
        start.toLocalDate().equals(end.toLocalDate()) ? end.format(TIME) : end.format(DATE_TIME);
    return head + "~" + tail;
  }

  private static String headline(Suspension suspension) {
    return suspension.getOrganization().getName() + " " + suspension.getBusiness().getLabel();
  }

  /** 새로 등록했다. 그 업무 결제원 담당자에게 알림, 메일, 매터모스트. */
  void registered(Suspension suspension, Collection<String> kftcEmails) {
    publish(
        kftcEmails,
        suspension,
        headline(suspension) + " 일시중단이 등록되었습니다",
        periodText(suspension.getStartAt(), suspension.getEndAt())
            + " · "
            + suspension.getReason()
            + " (등록 "
            + suspension.getRegistrantName()
            + "). 확인하고 조치 및 인수인계 사항을 써 주세요.",
        linkOf(suspension),
        EnumSet.of(NotificationChannel.EMAIL, NotificationChannel.MATTERMOST));
  }

  /** 내용이 바뀌었다. 결제원 담당자에게 매터모스트, 다시 검토가 필요하면 그렇게 적는다. */
  void changed(
      Suspension suspension,
      Collection<String> kftcEmails,
      String actorName,
      String what,
      boolean reviewReopened) {
    publish(
        kftcEmails,
        suspension,
        headline(suspension) + " 일시중단이 수정되었습니다",
        actorName
            + " 님이 "
            + what
            + "을(를) 고쳤습니다. "
            + periodText(suspension.getStartAt(), suspension.getEndAt())
            + (reviewReopened ? ". 다시 확인이 필요해 검토 대기로 돌아갔습니다." : "."),
        linkOf(suspension),
        EnumSet.of(NotificationChannel.MATTERMOST));
  }

  /** 지웠다. 결제원 담당자에게 매터모스트. 화면이 없어졌으므로 목록으로 잇는다. */
  void deleted(Suspension suspension, Collection<String> kftcEmails, String actorName) {
    publish(
        kftcEmails,
        suspension,
        headline(suspension) + " 일시중단이 삭제되었습니다",
        actorName
            + " 님이 "
            + periodText(suspension.getStartAt(), suspension.getEndAt())
            + " 일시중단("
            + suspension.getReason()
            + ")을 지웠습니다.",
        "/" + suspension.getBusiness().getSlug() + "/suspensions",
        EnumSet.of(NotificationChannel.MATTERMOST));
  }

  /** 등록한 사람에게: 결제원이 확정했다. 알림 센터에만. */
  void confirmed(Suspension suspension) {
    publish(
        Set.of(suspension.getRegistrantEmail()),
        suspension,
        headline(suspension) + " 일시중단이 확정되었습니다",
        periodText(suspension.getStartAt(), suspension.getEndAt())
            + " · "
            + suspension.getConfirmedByName()
            + " 님이 조치 및 인수인계 사항을 썼습니다.",
        linkOf(suspension),
        Set.of());
  }

  /** 등록한 사람에게: 다른 사람(결제원 담당자)이 내용을 고쳤다. 알림 센터에만. */
  void changedForRegistrant(Suspension suspension, String actorName, String what) {
    publish(
        Set.of(suspension.getRegistrantEmail()),
        suspension,
        headline(suspension) + " 일시중단을 결제원이 고쳤습니다",
        actorName + " 님이 " + what + "을(를) 고쳤습니다.",
        linkOf(suspension),
        Set.of());
  }

  private void publish(
      Collection<String> recipientEmails,
      Suspension suspension,
      String title,
      String body,
      String link,
      Set<NotificationChannel> channels) {
    for (String email : new LinkedHashSet<>(recipientEmails)) {
      if (email == null || email.isBlank()) {
        continue;
      }
      notificationService.publish(
          new NotificationCommand(
              email,
              NotificationType.SUSPENSION_CHANGED,
              suspension.getBusiness(),
              cut(title, TITLE_MAX),
              cut(body, BODY_MAX),
              link,
              channels));
    }
  }

  /** 알림 칼럼 길이(title 200, body 1000)에 맞춘다. */
  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max - 1) + "…";
  }
}
