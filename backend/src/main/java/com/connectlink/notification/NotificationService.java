package com.connectlink.notification;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.notification.dto.NotificationOutboxResponse;
import com.connectlink.notification.dto.NotificationResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 센터와 발송. 기능 모듈은 {@link #publish} 로 알림을 만들고, 화면은 조회·읽음 처리 API 를 쓴다. 다른 모듈은 Repository 대신 이
 * Service 를 쓴다.
 */
@Service
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationOutboxRepository outboxRepository;
  private final NotificationSender notificationSender;
  private final NotificationAccessPolicy accessPolicy;

  public NotificationService(
      NotificationRepository notificationRepository,
      NotificationOutboxRepository outboxRepository,
      NotificationSender notificationSender,
      NotificationAccessPolicy accessPolicy) {
    this.notificationRepository = notificationRepository;
    this.outboxRepository = outboxRepository;
    this.notificationSender = notificationSender;
    this.accessPolicy = accessPolicy;
  }

  /**
   * 알림 센터에 남기고, 요청된 채널로는 {@link NotificationSender} 를 통해 보낸다. 메일 제목에는 {@link MailSubject}
   * 머리말("[금융결제원] (업무명)")을 붙인다.
   */
  public NotificationResponse publish(NotificationCommand command) {
    return publish(command, MailSubject.of(command.business(), command.title()));
  }

  /**
   * {@link #publish} 와 같지만 메일 제목에 머리말을 붙이지 않고 알림 제목 그대로 보낸다. 보내는 사람이 미리보기에서 정한 리마인드 제목처럼 제목을 사람이 정한
   * 메일에 쓴다.
   */
  public NotificationResponse publishAsWritten(NotificationCommand command) {
    return publish(command, command.title());
  }

  private NotificationResponse publish(NotificationCommand command, String mailSubject) {
    Notification saved =
        notificationRepository.save(
            new Notification(
                command.recipientEmail(),
                command.type(),
                command.business(),
                command.title(),
                command.body(),
                command.link()));
    for (NotificationChannel channel : command.channels()) {
      notificationSender.send(
          new OutboundMessage(
              channel,
              recipientFor(channel, command.recipientEmail()),
              channel == NotificationChannel.EMAIL ? mailSubject : command.title(),
              command.body(),
              saved.getId()));
    }
    return NotificationResponse.from(saved);
  }

  /**
   * 사람이 아닌 받는 곳(예: 매터모스트 운영팀 채널)으로 바로 보낸다. 알림 센터에는 남지 않는다. 일시중단 전 영업일 취합처럼 팀 채널로 가는 전달에 쓴다.
   *
   * @param recipient 채널 주소(예: 매터모스트 {@code ~채널이름})
   */
  public void sendToChannel(
      NotificationChannel channel, String recipient, String subject, String body) {
    notificationSender.send(new OutboundMessage(channel, recipient, subject, body, null));
  }

  /** 매터모스트 대상은 알림 대상 설정(기능 프로젝트 몫)이 생기기 전까지 이메일 앞부분을 @handle 로 쓴다. */
  private static String recipientFor(NotificationChannel channel, String email) {
    if (channel == NotificationChannel.MATTERMOST) {
      int at = email.indexOf('@');
      return "@" + (at > 0 ? email.substring(0, at) : email);
    }
    return email;
  }

  @Transactional(readOnly = true)
  public PageResponse<NotificationResponse> list(
      CurrentUser user, boolean unreadOnly, Pageable pageable) {
    Page<Notification> page =
        unreadOnly
            ? notificationRepository.findByRecipientEmailAndReadAtIsNullOrderByCreatedAtDescIdDesc(
                user.email(), pageable)
            : notificationRepository.findByRecipientEmailOrderByCreatedAtDescIdDesc(
                user.email(), pageable);
    return PageResponse.from(page, NotificationResponse::from);
  }

  @Transactional(readOnly = true)
  public long unreadCount(CurrentUser user) {
    return notificationRepository.countByRecipientEmailAndReadAtIsNull(user.email());
  }

  public NotificationResponse markRead(CurrentUser user, Long id) {
    Notification notification =
        notificationRepository
            .findById(id)
            .orElseThrow(
                () -> BusinessException.notFound("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."));
    accessPolicy.ensureCanRead(user, notification);
    notification.markRead(LocalDateTime.now());
    return NotificationResponse.from(notification);
  }

  /** 읽지 않은 알림을 모두 읽음으로 바꾼다. 건별로 바꿔 감사 컬럼이 함께 갱신되게 한다. */
  public int markAllRead(CurrentUser user) {
    List<Notification> unread =
        notificationRepository.findByRecipientEmailAndReadAtIsNull(user.email());
    LocalDateTime now = LocalDateTime.now();
    unread.forEach(n -> n.markRead(now));
    return unread.size();
  }

  /**
   * 한 사람의 알림 센터 알림을 모두 지운다(탈퇴). 메일·매터모스트 발송함 기록은 발송 이력이라 남기고 알림과의 연결만 끊는다.
   *
   * @return 지운 알림 수
   */
  public int deleteAllFor(String recipientEmail) {
    List<Long> ids = notificationRepository.findIdsByRecipientEmail(recipientEmail);
    if (ids.isEmpty()) {
      return 0;
    }
    outboxRepository.detachNotifications(ids);
    notificationRepository.deleteAllByIdInBatch(ids);
    return ids.size();
  }

  @Transactional(readOnly = true)
  public PageResponse<NotificationOutboxResponse> listOutbox(CurrentUser user, Pageable pageable) {
    accessPolicy.ensureCanViewOutbox(user);
    return PageResponse.from(
        outboxRepository.findAllByOrderByCreatedAtDescIdDesc(pageable),
        NotificationOutboxResponse::from);
  }

  /**
   * 데모 도구: 한 받는 사람에게 간 최근 메일 5건(최신순). 가입 화면이 로그인 전에 인증 코드 메일을 보여 줄 때 쓴다. 운영에는 이 기능을 부르는 API 가 없다.
   */
  @Transactional(readOnly = true)
  public List<NotificationOutboxResponse> listRecentEmailsTo(String recipient) {
    return outboxRepository
        .findTop5ByChannelAndRecipientOrderByIdDesc(NotificationChannel.EMAIL, recipient)
        .stream()
        .map(NotificationOutboxResponse::from)
        .toList();
  }
}
