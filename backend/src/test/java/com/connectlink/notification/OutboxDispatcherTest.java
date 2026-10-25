package com.connectlink.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

/**
 * 발송함의 실제 발송: 발송 대기 건을 메일·매터모스트로 보내고 상태를 바꾼다. 메일 서버는 가짜 {@link JavaMailSenderImpl}, 매터모스트는 이 테스트가
 * 띄운 웹훅 서버다. 발송함을 실제로 바꾸므로 따로 쓰는 DB 다.
 */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:outbox-dispatch-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "app.notification.delivery.mail.from=noreply@connect-link.example.com",
      "app.notification.delivery.max-attempts=2"
    })
@ActiveProfiles("test")
@Import(OutboxDispatcherTest.FakeMail.class)
class OutboxDispatcherTest {

  /** 받은 메일을 모아 두고, 받는 사람이 fail 로 시작하면 실패하는 메일 서버. */
  @TestConfiguration
  static class FakeMail {
    static final List<SimpleMailMessage> SENT = new CopyOnWriteArrayList<>();

    @Bean
    JavaMailSenderImpl javaMailSender() {
      return new JavaMailSenderImpl() {
        @Override
        public void send(SimpleMailMessage message) {
          if (message.getTo()[0].startsWith("fail")) {
            throw new org.springframework.mail.MailSendException("메일 서버가 거부했습니다");
          }
          SENT.add(message);
        }
      };
    }
  }

  @Autowired private OutboxDispatcher dispatcher;
  @Autowired private NotificationSender sender;
  @Autowired private NotificationOutboxRepository outboxRepository;

  @BeforeEach
  void setUp() throws Exception {
    // 앞 테스트·샘플 데이터의 발송 대기 건은 이 테스트와 상관없으므로 보낸 것으로 치운다.
    List<NotificationOutbox> pending = outboxRepository.findAll();
    pending.forEach(o -> o.markSent(LocalDateTime.now()));
    outboxRepository.saveAll(pending);
    FakeMail.SENT.clear();
  }

  private NotificationOutbox latest(String recipient) {
    return outboxRepository.findAll().stream()
        .filter(o -> o.getRecipient().equals(recipient))
        .reduce((a, b) -> b)
        .orElseThrow();
  }

  @Test
  void 발송_대기_메일을_보내고_발송됨으로_바꾼다() {
    sender.send(
        new OutboundMessage(
            NotificationChannel.EMAIL, "kim@kb.example.com", "[연결이] 제목", "본문", null));

    assertThat(dispatcher.dispatch()).isEqualTo(1);

    assertThat(latest("kim@kb.example.com").getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(FakeMail.SENT).hasSize(1);
    assertThat(FakeMail.SENT.get(0).getFrom()).isEqualTo("noreply@connect-link.example.com");
    assertThat(FakeMail.SENT.get(0).getSubject()).isEqualTo("[연결이] 제목");
    assertThat(dispatcher.dispatch()).isZero();
  }

  @Test
  void 실패하면_까닭을_남기고_정해진_횟수를_넘기면_실패로_끝낸다() {
    sender.send(
        new OutboundMessage(NotificationChannel.EMAIL, "fail@kb.example.com", "제목", "본문", null));

    dispatcher.dispatch();
    NotificationOutbox first = latest("fail@kb.example.com");
    assertThat(first.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(first.getAttempts()).isEqualTo(1);
    assertThat(first.getLastError()).contains("거부");

    dispatcher.dispatch();
    NotificationOutbox second = latest("fail@kb.example.com");
    assertThat(second.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(second.getAttempts()).isEqualTo(2);
  }

  @Test
  void 매터모스트는_보내지_않고_발송함에_기록만_남긴다() {
    sender.send(new OutboundMessage(NotificationChannel.MATTERMOST, "@kim", "제목", "본문", null));

    assertThat(dispatcher.dispatch()).isZero();
    assertThat(latest("@kim").getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(latest("@kim").getAttempts()).isZero();
  }
}
