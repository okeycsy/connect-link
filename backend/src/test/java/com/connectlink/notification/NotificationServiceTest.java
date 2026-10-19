package com.connectlink.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.connectlink.notification.dto.NotificationResponse;
import com.connectlink.organization.Business;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

  @Autowired private NotificationService notificationService;
  @Autowired private NotificationOutboxRepository outboxRepository;

  @Test
  void 발행하면_알림_센터에_남고_채널마다_발송함에_기록된다() throws Exception {
    NotificationResponse created =
        notificationService.publish(
            new NotificationCommand(
                "kim.minjun@kb.example.com",
                NotificationType.SUSPENSION_CHANGED,
                Business.CMS,
                "KB국민은행 CMS 일시중단이 등록되었습니다",
                "2026-10-20 01:00~03:00",
                "/cms/suspensions",
                Set.of(NotificationChannel.EMAIL, NotificationChannel.MATTERMOST)));

    assertThat(created.id()).isNotNull();
    assertThat(created.read()).isFalse();
    assertThat(created.businessSlug()).isEqualTo("cms");

    List<NotificationOutbox> rows =
        outboxRepository.findAll().stream()
            .filter(o -> created.id().equals(o.getNotificationId()))
            .toList();
    assertThat(rows).hasSize(2);
    assertThat(rows).extracting(NotificationOutbox::getStatus).containsOnly(OutboxStatus.PENDING);
    assertThat(rows)
        .extracting(NotificationOutbox::getChannel, NotificationOutbox::getRecipient)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(
                NotificationChannel.EMAIL, "kim.minjun@kb.example.com"),
            org.assertj.core.groups.Tuple.tuple(NotificationChannel.MATTERMOST, "@kim.minjun"));
  }

  @Test
  void 채널이_없으면_알림_센터에만_남는다() throws Exception {
    long before = outboxRepository.count();
    notificationService.publish(
        NotificationCommand.inAppOnly(
            "admin@kftc.example.com", NotificationType.SYSTEM, null, "안내", null, null));
    assertThat(outboxRepository.count()).isEqualTo(before);
  }
}
