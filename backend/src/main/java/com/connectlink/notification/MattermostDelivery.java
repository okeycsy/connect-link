package com.connectlink.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 매터모스트 발송. 들어오는 웹훅 하나로 보내고, 받는 곳은 웹훅의 {@code channel} 덮어쓰기로 정한다: 사람은 {@code @아이디}(개인 메시지), 채널은
 * {@code ~채널이름} 에서 물결을 뗀 채널 이름이다. 웹훅이 채널 덮어쓰기를 허용해야 한다(매터모스트 관리 설정).
 */
@Component
class MattermostDelivery implements ChannelDelivery {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final NotificationDeliveryProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

  MattermostDelivery(NotificationDeliveryProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
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
  public void deliver(NotificationOutbox outbox) throws Exception {
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("channel", channelOf(outbox.getRecipient()));
    payload.put("username", properties.getMattermost().getUsername());
    payload.put("text", textOf(outbox));
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(properties.getMattermost().getWebhookUrl()))
            .timeout(TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
            .build();
    HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() / 100 != 2) {
      throw new IllegalStateException(
          "매터모스트 웹훅 응답 " + response.statusCode() + ": " + response.body());
    }
  }

  /** {@code ~kftc-ops} → {@code kftc-ops}. 사람({@code @아이디})은 그대로 둔다. */
  static String channelOf(String recipient) {
    return recipient.startsWith("~") ? recipient.substring(1) : recipient;
  }

  /** 제목이 있으면 굵게 첫 줄에 두고 본문을 잇는다. */
  static String textOf(NotificationOutbox outbox) {
    String body = outbox.getBody() == null ? "" : outbox.getBody();
    if (outbox.getSubject() == null || outbox.getSubject().isBlank()) {
      return body;
    }
    return "**" + outbox.getSubject() + "**" + (body.isBlank() ? "" : "\n" + body);
  }
}
