package com.connectlink.opinion;

import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationCommand;
import com.connectlink.notification.NotificationService;
import com.connectlink.notification.NotificationType;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 의견 요청 모듈의 알림. 모두 {@link NotificationService} 를 거쳐 알림 센터에 남고, 메일이 필요한 것은 메일 채널을 더한다(프로토타입은 발송함에
 * 기록). 받는 사람은 대상 기관의 회신 담당자이거나 요청을 만든 결제원 담당자다.
 */
@Component
class OpinionNotifier {

  private static final int TITLE_MAX = 200;
  private static final int BODY_MAX = 1000;

  private final NotificationService notificationService;

  OpinionNotifier(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /** 의견 요청 화면 주소. */
  static String linkOf(OpinionRequest request) {
    return "/" + request.getBusiness().getSlug() + "/opinions/" + request.getId();
  }

  /** 회신중으로 열렸다(또는 회신중 요청에 새로 대상·담당자로 들어갔다). 알림과 메일. */
  void opened(OpinionRequest request, Collection<String> recipientEmails) {
    String body =
        request.getOwnerName()
            + " 님이 "
            + request.getBusiness().getLabel()
            + " 의견을 요청했습니다. 회신 기한은 "
            + request.getDueDate()
            + " 입니다. 연결이에서 요청 내용을 보고 질문에 답해 회신해 주세요.";
    publish(
        recipientEmails,
        NotificationType.OPINION_REQUEST_OPENED,
        request,
        "의견 요청: " + request.getTitle(),
        body,
        Set.of(NotificationChannel.EMAIL));
  }

  /** 회신중 요청의 요청명·내용·기한이 바뀌었다. 알림 센터에만 남긴다. */
  void changed(OpinionRequest request, Collection<String> recipientEmails, String what) {
    publish(
        recipientEmails,
        NotificationType.OPINION_REQUEST_CHANGED,
        request,
        "의견 요청이 바뀌었습니다: " + request.getTitle(),
        what + "이(가) 바뀌었습니다. 회신 기한은 " + request.getDueDate() + " 입니다.",
        Set.of());
  }

  /** 회신중 요청을 취소했다. 알림과 메일. */
  void cancelled(OpinionRequest request, Collection<String> recipientEmails) {
    publish(
        recipientEmails,
        NotificationType.OPINION_REQUEST_CHANGED,
        request,
        "의견 요청이 취소되었습니다: " + request.getTitle(),
        "취소 사유: " + request.getCancelReason() + " 이미 작성한 회신은 따로 내지 않아도 됩니다.",
        Set.of(NotificationChannel.EMAIL));
  }

  /** 메일 제목과 본문 한 벌. */
  record Message(String title, String body) {}

  /** 미회신 기관에 보내는 리마인드의 기본 문구. 보내기 전 미리보기도 이 문구를 보여 준다. */
  Message reminderMessage(OpinionRequest request, long daysLeft) {
    String due =
        daysLeft > 0
            ? "회신 기한까지 " + daysLeft + "일 남았습니다"
            : daysLeft == 0 ? "오늘이 회신 기한입니다" : "회신 기한이 " + (-daysLeft) + "일 지났습니다";
    return new Message(
        cut("회신을 기다립니다: " + request.getTitle(), TITLE_MAX),
        cut(
            due + "(기한 " + request.getDueDate() + "). 아직 회신이 제출되지 않았습니다. 연결이에서 회신해 주세요.",
            BODY_MAX));
  }

  /** 미회신 기관에 보내는 리마인드. 알림과 메일. 문구는 기본 문구이거나 보내는 사람이 고친 것이다. */
  void remind(OpinionRequest request, Collection<String> recipientEmails, Message message) {
    publish(
        recipientEmails,
        NotificationType.OPINION_REPLY_DUE,
        request,
        message.title(),
        message.body(),
        Set.of(NotificationChannel.EMAIL));
  }

  /** 기관이 제출한 회신을 고치고 싶다고 요청했다. 요청을 만든 결제원 담당자에게 알린다. */
  void revisionRequested(OpinionRequest request, OpinionReply reply) {
    publish(
        Set.of(request.getOwnerEmail()),
        NotificationType.OPINION_REPLY_REVISION,
        request,
        reply.getTarget().getOrganization().getName() + " 회신 수정 요청: " + request.getTitle(),
        reply.getReplierName() + " 님이 제출한 회신을 고치려고 합니다. 사유: " + reply.getRevisionReason(),
        Set.of());
  }

  /** 수정 요청을 승인했거나 받아들이지 않았다. 요청한 기관 담당자에게 알린다. */
  void revisionReviewed(OpinionRequest request, OpinionReply reply, boolean approved) {
    String body =
        approved
            ? "결제원 담당자가 수정 요청을 승인했습니다. 회신을 고쳐 다시 제출해 주세요."
            : "결제원 담당자가 수정 요청을 받아들이지 않았습니다. 사유: " + reply.getRevisionRejectionReason();
    publish(
        Set.of(reply.getReplierEmail()),
        NotificationType.OPINION_REPLY_REVISION,
        request,
        (approved ? "회신 수정 요청 승인: " : "회신 수정 요청 반려: ") + request.getTitle(),
        body,
        Set.of());
  }

  private void publish(
      Collection<String> recipientEmails,
      NotificationType type,
      OpinionRequest request,
      String title,
      String body,
      Set<NotificationChannel> channels) {
    for (String email : new LinkedHashSet<>(recipientEmails)) {
      if (email == null || email.isBlank()) {
        continue;
      }
      notificationService.publish(
          new NotificationCommand(
              email,
              type,
              request.getBusiness(),
              cut(title, TITLE_MAX),
              cut(body, BODY_MAX),
              linkOf(request),
              channels));
    }
  }

  /** 알림 칼럼 길이(title 200, body 1000)에 맞춘다. 요청명이 길면 제목 끝이 잘린다. */
  private static String cut(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max - 1) + "…";
  }
}
