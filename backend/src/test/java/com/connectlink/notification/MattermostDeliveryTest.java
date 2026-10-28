package com.connectlink.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/** 매터모스트 Incoming Webhook 발송. 웹훅 서버는 가짜({@link MockRestServiceServer})다. */
class MattermostDeliveryTest {

  private static final String WEBHOOK = "https://mattermost.example.com/hooks/test";

  private final NotificationDeliveryProperties properties = new NotificationDeliveryProperties();
  private final RestTemplate restTemplate = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
  private final MattermostDelivery delivery = new MattermostDelivery(properties, restTemplate);

  @BeforeEach
  void setUp() {
    properties.getMattermost().setWebhookUrl(WEBHOOK);
  }

  private static NotificationOutbox outbox(String recipient, String subject, String body) {
    return new NotificationOutbox(null, NotificationChannel.MATTERMOST, recipient, subject, body);
  }

  @Test
  void 웹훅_주소가_없으면_보낼_수_없다() {
    properties.getMattermost().setWebhookUrl("");
    assertThat(delivery.available()).isFalse();
  }

  @Test
  void 팀_채널_글만_받고_개인_앞_글은_받지_않는다() {
    assertThat(delivery.accepts(outbox("~kftc-ops", "제목", "본문"))).isTrue();
    assertThat(delivery.accepts(outbox("@kim", "제목", "본문"))).isFalse();
  }

  @Test
  void 채널_이름과_굵은_제목_본문을_웹훅으로_올린다() {
    server
        .expect(requestTo(WEBHOOK))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    "{\"channel\":\"kftc-ops\",\"username\":\"연결이\","
                        + "\"text\":\"**[연결이] 일시중단 확정**\\n\\n- 관련 업무: CMS\"}"))
        .andRespond(withSuccess("ok", null));

    delivery.deliver(outbox("~kftc-ops", "[연결이] 일시중단 확정", "- 관련 업무: CMS"));

    server.verify();
  }

  @Test
  void 웹훅이_실패하면_예외를_던져_발송기가_다시_해_보게_한다() {
    server.expect(requestTo(WEBHOOK)).andRespond(withServerError());

    assertThatThrownBy(() -> delivery.deliver(outbox("~kftc-ops", "제목", "본문")))
        .hasMessageContaining("매터모스트가 거절했습니다(500)");
  }

  @Test
  void 연결하지_못하면_웹훅_주소를_빼고_까닭을_남긴다() {
    server
        .expect(requestTo(WEBHOOK))
        .andRespond(
            request -> {
              throw new java.net.UnknownHostException("mattermost.example.com");
            });

    assertThatThrownBy(() -> delivery.deliver(outbox("~kftc-ops", "제목", "본문")))
        .hasMessageContaining("연결하지 못했습니다: UnknownHostException mattermost.example.com")
        .hasMessageNotContaining("/hooks/");
  }
}
