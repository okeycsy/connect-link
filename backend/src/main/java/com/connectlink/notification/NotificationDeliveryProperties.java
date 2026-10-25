package com.connectlink.notification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 발송함의 실제 발송 설정 ({@code app.notification.delivery.*}). 기본은 꺼져 있어(demo·local) 발송함에 기록만 한다. 운영은 켜고 메일
 * 서버({@code spring.mail.*})와 매터모스트 웹훅 주소를 넣는다. 공개 저장소라 실제 값은 배포 설정에만 둔다.
 *
 * <ul>
 *   <li>{@code enabled}: 실제 발송을 할지. 기본 false.
 *   <li>{@code interval}: 발송함을 훑는 간격. 기본 30초.
 *   <li>{@code batch-size}: 한 번에 보낼 최대 건수. 기본 50.
 *   <li>{@code max-attempts}: 실패하면 다시 해 볼 횟수(처음 포함). 넘으면 실패로 끝낸다. 기본 3.
 *   <li>{@code mail.from}: 보내는 사람 주소. 비우면 메일은 보내지 않고 발송 대기로 둔다.
 *   <li>{@code mattermost.webhook-url}: 매터모스트 들어오는 웹훅 주소. 비우면 매터모스트는 발송 대기로 둔다.
 *   <li>{@code mattermost.username}: 매터모스트에 보일 보낸 이 이름. 기본 "연결이".
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.notification.delivery")
public class NotificationDeliveryProperties {

  private boolean enabled = false;
  private Duration interval = Duration.ofSeconds(30);
  private int batchSize = 50;
  private int maxAttempts = 3;
  private final Mail mail = new Mail();
  private final Mattermost mattermost = new Mattermost();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Duration getInterval() {
    return interval;
  }

  public void setInterval(Duration interval) {
    this.interval = interval;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public Mail getMail() {
    return mail;
  }

  public Mattermost getMattermost() {
    return mattermost;
  }

  public static class Mail {
    private String from = "";

    public String getFrom() {
      return from;
    }

    public void setFrom(String from) {
      this.from = from;
    }
  }

  public static class Mattermost {
    private String webhookUrl = "";
    private String username = "연결이";

    public String getWebhookUrl() {
      return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
      this.webhookUrl = webhookUrl;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }
  }
}
