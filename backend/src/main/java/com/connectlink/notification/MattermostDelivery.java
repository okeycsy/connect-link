package com.connectlink.notification;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 매터모스트 발송. Incoming Webhook 주소({@code app.notification.delivery.mattermost.webhook-url})로 글을 올린다.
 * 받는 곳이 팀 채널({@code ~채널이름})인 건만 보내고 그 채널 이름을 글의 채널로 넘긴다(웹훅을 한 채널에 고정하지 않아야 한다). 개인
 * 앞({@code @handle}) 글은 받는 사람의 매터모스트 계정을 잇는 설정이 아직 없어 발송함에 기록만 남긴다. 제목은 굵게 첫 줄에, 본문은 그 아래에 둔다.
 */
@Component
class MattermostDelivery implements ChannelDelivery {

  private final NotificationDeliveryProperties properties;
  private final RestTemplate restTemplate;

  @Autowired
  MattermostDelivery(NotificationDeliveryProperties properties, RestTemplateBuilder builder) {
    this(
        properties,
        builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build());
  }

  /** 테스트가 가짜 웹훅 서버를 붙인 {@link RestTemplate} 을 넘긴다. */
  MattermostDelivery(NotificationDeliveryProperties properties, RestTemplate restTemplate) {
    this.properties = properties;
    this.restTemplate = restTemplate;
  }

  @Override
  public NotificationChannel channel() {
    return NotificationChannel.MATTERMOST;
  }

  @Override
  public boolean available() {
    return !properties.getMattermost().getWebhookUrl().isBlank();
  }

  @Override
  public boolean accepts(NotificationOutbox outbox) {
    return outbox.getRecipient().startsWith("~");
  }

  @Override
  public void deliver(NotificationOutbox outbox) {
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("channel", outbox.getRecipient().substring(1));
    payload.put("username", properties.getMattermost().getUsername());
    payload.put("text", text(outbox));
    restTemplate.postForEntity(properties.getMattermost().getWebhookUrl(), payload, String.class);
  }

  private static String text(NotificationOutbox outbox) {
    String body = outbox.getBody() == null ? "" : outbox.getBody();
    if (outbox.getSubject() == null || outbox.getSubject().isBlank()) {
      return body;
    }
    return "**" + outbox.getSubject() + "**\n\n" + body;
  }
}
