package com.connectlink.suspension;

import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import com.connectlink.organization.Business;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * 일시중단 모듈의 알림. 모두 {@link NotificationService} 를 거쳐 알림 센터에 남고, 채널은 명세를 따른다: 새로 등록하면 멈추는 업무들의 결제원
 * 담당자에게 메일과 매터모스트(그 순서), 고치거나 지우면 매터모스트. 확정하면 등록한 사람에게 알림 센터와 메일, 일시중단의 기관 담당자 연락처에 메일을 보낸다. 결제원이
 * 고친 소식은 등록한 사람에게 알림 센터로만 알린다.
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

  /** 일시중단 화면 주소. 여러 업무를 멈추는 건은 받는 사람이 맡은 업무 영역으로 연다. */
  static String linkOf(Suspension suspension, Business business) {
    return "/" + business.getSlug() + "/suspensions/" + suspension.getId();
  }

  /** "2026-10-18 02:00~04:00" 꼴. 날짜가 다르면 끝에도 날짜를 붙인다. */
  static String periodText(LocalDateTime start, LocalDateTime end) {
    String head = start.format(DATE_TIME);
    String tail =
        start.toLocalDate().equals(end.toLocalDate()) ? end.format(TIME) : end.format(DATE_TIME);
    return head + "~" + tail;
  }

  private static String headline(Suspension suspension) {
    return suspension.getOrganization().getName() + " " + suspension.targetWorkNames();
  }

  /** 새로 등록했다. 그 업무 결제원 담당자에게 알림, 메일, 매터모스트. */
  void registered(Suspension suspension, Map<String, Business> kftcEmails) {
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
        b -> linkOf(suspension, b),
        EnumSet.of(NotificationChannel.EMAIL, NotificationChannel.MATTERMOST));
  }

  /** 내용이 바뀌었다. 결제원 담당자에게 매터모스트, 다시 검토가 필요하면 그렇게 적는다. */
  void changed(
      Suspension suspension,
      Map<String, Business> kftcEmails,
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
        b -> linkOf(suspension, b),
        EnumSet.of(NotificationChannel.MATTERMOST));
  }

  /** 지웠다. 결제원 담당자에게 매터모스트. 화면이 없어졌으므로 목록으로 잇는다. */
  void deleted(Suspension suspension, Map<String, Business> kftcEmails, String actorName) {
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
        b -> "/" + b.getSlug() + "/suspensions",
        EnumSet.of(NotificationChannel.MATTERMOST));
  }

  /**
   * 결제원이 확정했다. 등록한 사람(확정한 사람이 아니면)에게 알림 센터와 메일, 일시중단에 적힌 기관 담당자 연락처에는 메일만 보낸다(계정이 없을 수 있다).
   *
   * @param registrant 등록한 사람과 알림을 보일 업무 영역. 확정한 사람이 등록했으면 비어 있다
   * @param skipEmail 확정한 사람의 이메일. 연락처에 있어도 보내지 않는다
   */
  void confirmed(Suspension suspension, Map<String, Business> registrant, String skipEmail) {
    String title = headline(suspension) + " 일시중단이 확정되었습니다";
    String body =
        periodText(suspension.getStartAt(), suspension.getEndAt())
            + " · "
            + suspension.getConfirmedByName()
            + " 님이 조치 및 인수인계 사항을 썼습니다. 연결이 일시중단 화면에서 확인해 주세요.";
    publish(
        registrant,
        suspension,
        title,
        body,
        b -> linkOf(suspension, b),
        EnumSet.of(NotificationChannel.EMAIL));
    Set<String> sent = new HashSet<>(registrant.keySet());
    sent.add(skipEmail);
    for (SuspensionContact contact : suspension.getContacts()) {
      String email = contact.getEmail();
      if (email == null || email.isBlank() || !sent.add(email)) {
        continue;
      }
      notificationService.sendToChannel(
          NotificationChannel.EMAIL, email, cut(title, TITLE_MAX), cut(body, BODY_MAX));
    }
  }

  /** 등록한 사람에게: 다른 사람(결제원 담당자)이 내용을 고쳤다. 알림 센터에만. */
  void changedForRegistrant(
      Suspension suspension, Map<String, Business> registrant, String actorName, String what) {
    publish(
        registrant,
        suspension,
        headline(suspension) + " 일시중단을 결제원이 고쳤습니다",
        actorName + " 님이 " + what + "을(를) 고쳤습니다.",
        b -> linkOf(suspension, b),
        Set.of());
  }

  /**
   * @param recipients 받는 사람 이메일과 알림을 보일 업무 영역
   * @param link 그 업무 영역의 화면 주소
   */
  private void publish(
      Map<String, Business> recipients,
      Suspension suspension,
      String title,
      String body,
      Function<Business, String> link,
      Set<NotificationChannel> channels) {
    recipients.forEach(
        (email, business) -> {
          if (email == null || email.isBlank()) {
            return;
          }
          notificationService.publish(
              new NotificationCommand(
                  email,
                  NotificationType.SUSPENSION_CHANGED,
                  business,
                  cut(title, TITLE_MAX),
                  cut(body, BODY_MAX),
                  link.apply(business),
                  channels));
        });
  }

  /** 알림 칼럼 길이(title 200, body 1000)에 맞춘다. */
  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max - 1) + "…";
  }
}
