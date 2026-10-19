package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationService;
import com.connectlink.suspension.dto.SuspensionDigestResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전 영업일 취합 전달. 영업일마다 그 다음 영업일까지(사이의 주말·휴일 포함) 시작하는 확정 일시중단을 모아 운영·관제팀 매터모스트 채널로 보낸다. 글 양식은 {@link
 * SuspensionDigestFormatter}, 채널과 휴일은 {@link SuspensionDigestProperties} 가 정한다. 실제 발송은 알림 모듈의 발송
 * 인터페이스를 거친다(프로토타입은 발송함 기록).
 */
@Service
@Transactional
public class SuspensionDigestService {

  private final SuspensionRepository suspensionRepository;
  private final SuspensionDigestRepository digestRepository;
  private final SuspensionDigestFormatter formatter;
  private final SuspensionDigestProperties properties;
  private final BusinessDayCalendar calendar;
  private final NotificationService notificationService;
  private final SuspensionAccessPolicy accessPolicy;
  private final Clock clock;

  public SuspensionDigestService(
      SuspensionRepository suspensionRepository,
      SuspensionDigestRepository digestRepository,
      SuspensionDigestFormatter formatter,
      SuspensionDigestProperties properties,
      BusinessDayCalendar calendar,
      NotificationService notificationService,
      SuspensionAccessPolicy accessPolicy,
      Clock clock) {
    this.suspensionRepository = suspensionRepository;
    this.digestRepository = digestRepository;
    this.formatter = formatter;
    this.properties = properties;
    this.calendar = calendar;
    this.notificationService = notificationService;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  /** 예약 작업: 오늘이 영업일이고 아직 보내지 않았으면 보낸다. */
  public Optional<SuspensionDigestResponse> sendScheduled() {
    LocalDate today = LocalDate.now(clock);
    if (!calendar.isBusinessDay(today) || digestRepository.findBySentOn(today).isPresent()) {
      return Optional.empty();
    }
    return Optional.of(send(today));
  }

  /**
   * 수동으로 보내기: 오늘을 보내는 날로 보고 취합을 바로 보낸다(영업일이 아니어도 보낸다). 예약 발송 전에 보내면 그날 예약 발송은 건너뛰고, 오늘 보낸 기록이 있으면 새
   * 기록으로 바꾼다. 결제원 담당자와 관리자만 부른다.
   */
  public SuspensionDigestResponse sendNow(CurrentUser user) {
    accessPolicy.ensureCanSendDigest(user);
    LocalDate today = LocalDate.now(clock);
    digestRepository
        .findBySentOn(today)
        .ifPresent(
            d -> {
              digestRepository.delete(d);
              digestRepository.flush();
            });
    return send(today);
  }

  private SuspensionDigestResponse send(LocalDate sentOn) {
    LocalDate fromDate = sentOn.plusDays(1);
    LocalDate toDate = calendar.nextBusinessDay(sentOn);
    List<Suspension> confirmed =
        suspensionRepository.findStartingBetween(
            SuspensionStatus.CONFIRMED, fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());
    String subject = formatter.subject(fromDate, toDate, confirmed.size());
    String body = formatter.body(fromDate, toDate, confirmed);
    List<String> channels = List.copyOf(properties.getChannels());
    for (String channel : channels) {
      notificationService.sendToChannel(NotificationChannel.MATTERMOST, channel, subject, body);
    }
    LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    SuspensionDigest digest =
        digestRepository.save(
            new SuspensionDigest(sentOn, fromDate, toDate, confirmed.size(), channels, body, now));
    return new SuspensionDigestResponse(
        digest.getSentOn(),
        digest.getFromDate(),
        digest.getToDate(),
        digest.getItemCount(),
        digest.getRecipients(),
        digest.getBody(),
        digest.getSentAt());
  }
}
