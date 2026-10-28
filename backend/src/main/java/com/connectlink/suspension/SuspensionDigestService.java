package com.connectlink.suspension;

import com.connectlink.auth.BusinessAccessPolicy;
import com.connectlink.auth.CurrentUser;
import com.connectlink.notification.NotificationChannel;
import com.connectlink.notification.NotificationService;
import com.connectlink.organization.Business;
import com.connectlink.suspension.dto.SuspensionDigestResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
  private final BusinessAccessPolicy businessAccessPolicy;
  private final Clock clock;

  public SuspensionDigestService(
      SuspensionRepository suspensionRepository,
      SuspensionDigestRepository digestRepository,
      SuspensionDigestFormatter formatter,
      SuspensionDigestProperties properties,
      BusinessDayCalendar calendar,
      NotificationService notificationService,
      SuspensionAccessPolicy accessPolicy,
      BusinessAccessPolicy businessAccessPolicy,
      Clock clock) {
    this.suspensionRepository = suspensionRepository;
    this.digestRepository = digestRepository;
    this.formatter = formatter;
    this.properties = properties;
    this.calendar = calendar;
    this.notificationService = notificationService;
    this.accessPolicy = accessPolicy;
    this.businessAccessPolicy = businessAccessPolicy;
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

  /**
   * 확정 즉시 알림: 처음 확정된 건 하나를 관련 업무와 조치 및 인수인계 사항과 함께 취합 채널로 바로 보낸다({@code send-on-confirm}). 전 영업일 취합
   * 기록에는 남기지 않는다.
   */
  public void sendConfirmed(Suspension suspension) {
    if (!properties.isSendOnConfirm()) {
      return;
    }
    String subject = formatter.confirmedSubject(suspension);
    String body = formatter.confirmedBody(suspension);
    for (String channel : properties.getChannels()) {
      notificationService.sendToChannel(NotificationChannel.MATTERMOST, channel, subject, body);
    }
  }

  /**
   * 예약 작업: 오늘이 주간 취합을 보내는 날(그 주 마지막 영업일과 그 두 영업일 앞)이면 업무마다 그 업무 관련 건만 따로 보낸다. 확정된 건이 없는 업무는 보내지 않고,
   * 관련 업무가 없는 건은 "그 밖의 업무" 로 한 번 더 보낸다.
   */
  public List<SuspensionDigestResponse> sendWeeklyScheduled() {
    if (!calendar.isWeeklyDigestDay(LocalDate.now(clock))) {
      return List.of();
    }
    LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    List<Suspension> confirmed = confirmedThisWeek(now);
    List<SuspensionDigestResponse> sent = new ArrayList<>();
    for (Business business : Business.values()) {
      List<Suspension> related =
          confirmed.stream().filter(s -> s.getBusinesses().contains(business)).toList();
      if (!related.isEmpty()) {
        sent.add(sendWeekly(business.getLabel(), related, now));
      }
    }
    List<Suspension> unlinked =
        confirmed.stream().filter(s -> s.getBusinesses().isEmpty()).toList();
    if (!unlinked.isEmpty()) {
      sent.add(sendWeekly("그 밖의 업무", unlinked, now));
    }
    return sent;
  }

  /**
   * 주간 취합 수동으로 보내기: 요일과 상관없이 이번 주(월요일부터 지금까지) 확정된 건 중 이 업무 관련 건만 바로 보낸다. 그 업무 영역을 볼 수 있는 결제원 담당자와
   * 관리자만 부른다.
   */
  public SuspensionDigestResponse sendWeeklyNow(CurrentUser user, Business business) {
    accessPolicy.ensureCanSendDigest(user);
    businessAccessPolicy.ensureCanView(user, business);
    LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    List<Suspension> related =
        confirmedThisWeek(now).stream().filter(s -> s.getBusinesses().contains(business)).toList();
    return sendWeekly(business.getLabel(), related, now);
  }

  /** 이번 주 월요일 0시부터 지금까지 확정된(확정·완료) 일시중단. 전 업무. */
  private List<Suspension> confirmedThisWeek(LocalDateTime now) {
    return suspensionRepository.findConfirmedBetween(
        List.of(SuspensionStatus.CONFIRMED, SuspensionStatus.COMPLETED),
        BusinessDayCalendar.weekStart(now.toLocalDate()).atStartOfDay(),
        now.plusNanos(1));
  }

  /** 모은 건을 취합 채널로 보낸다. 전 영업일 취합 기록에는 남기지 않는다. */
  private SuspensionDigestResponse sendWeekly(
      String scope, List<Suspension> suspensions, LocalDateTime now) {
    LocalDate today = now.toLocalDate();
    LocalDate fromDate = BusinessDayCalendar.weekStart(today);
    String subject = formatter.weeklySubject(scope, fromDate, today, suspensions.size());
    String body = formatter.weeklyBody(scope, fromDate, today, suspensions);
    List<String> channels = List.copyOf(properties.getChannels());
    for (String channel : channels) {
      notificationService.sendToChannel(NotificationChannel.MATTERMOST, channel, subject, body);
    }
    return new SuspensionDigestResponse(
        today, fromDate, today, suspensions.size(), channels, body, now);
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
