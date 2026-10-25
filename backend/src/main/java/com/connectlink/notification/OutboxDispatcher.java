package com.connectlink.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 발송함의 실제 발송기(outbox 패턴). 업무 코드는 {@link NotificationSender} 로 발송함에 기록만 하고, 이 작업이 정해진 간격마다 발송 대기 건을
 * 채널별 {@link ChannelDelivery} 로 보낸다. {@code app.notification.delivery.enabled} 가 꺼져 있으면(기본) 아무것도 하지
 * 않는다. 한 건씩 따로 커밋해서 한 건이 실패해도 나머지는 보낸다.
 */
@Component
public class OutboxDispatcher {

  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

  private final NotificationOutboxRepository outboxRepository;
  private final NotificationDeliveryProperties properties;
  private final Map<NotificationChannel, ChannelDelivery> deliveries =
      new EnumMap<>(NotificationChannel.class);
  private final TransactionTemplate transaction;
  private final Clock clock;

  public OutboxDispatcher(
      NotificationOutboxRepository outboxRepository,
      NotificationDeliveryProperties properties,
      List<ChannelDelivery> deliveries,
      TransactionTemplate transaction,
      Clock clock) {
    this.outboxRepository = outboxRepository;
    this.properties = properties;
    deliveries.forEach(d -> this.deliveries.put(d.channel(), d));
    this.transaction = transaction;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${app.notification.delivery.interval:30000}",
      initialDelayString = "${app.notification.delivery.interval:30000}")
  public void scheduled() {
    if (properties.isEnabled()) {
      dispatch();
    }
  }

  /**
   * 발송 대기 건을 오래된 것부터 한 묶음 보낸다. 설정이 갖춰지지 않은 채널의 건은 건드리지 않는다.
   *
   * @return 이번에 보낸 건수
   */
  public int dispatch() {
    List<Long> ids =
        transaction.execute(
            status ->
                outboxRepository
                    .findByStatusOrderByIdAsc(
                        OutboxStatus.PENDING, PageRequest.of(0, properties.getBatchSize()))
                    .stream()
                    .filter(o -> deliverable(o.getChannel()))
                    .map(NotificationOutbox::getId)
                    .toList());
    int sent = 0;
    for (Long id : ids) {
      Boolean ok = transaction.execute(status -> dispatchOne(id));
      if (Boolean.TRUE.equals(ok)) {
        sent++;
      }
    }
    return sent;
  }

  private boolean deliverable(NotificationChannel channel) {
    ChannelDelivery delivery = deliveries.get(channel);
    return delivery != null && delivery.available();
  }

  private boolean dispatchOne(Long id) {
    NotificationOutbox outbox = outboxRepository.findById(id).orElse(null);
    if (outbox == null || outbox.getStatus() != OutboxStatus.PENDING) {
      return false;
    }
    try {
      deliveries.get(outbox.getChannel()).deliver(outbox);
      outbox.markSent(LocalDateTime.now(clock));
      return true;
    } catch (Exception e) {
      log.warn("발송 실패 outbox={} channel={}: {}", id, outbox.getChannel(), e.toString());
      outbox.markAttemptFailed(
          e.getMessage() == null ? e.toString() : e.getMessage(), properties.getMaxAttempts());
      return false;
    }
  }
}
