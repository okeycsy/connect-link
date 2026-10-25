package com.connectlink.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 알림 설정 ({@code app.notification.*}).
 *
 * <ul>
 *   <li>{@code site-url}: 메일 본문에 넣는 연결이 주소의 앞부분(예: {@code https://연결이.example.com}). 공개 저장소라 실제 주소는
 *       배포 설정에서만 넣는다.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

  private String siteUrl = "http://localhost:8080";

  public String getSiteUrl() {
    return siteUrl;
  }

  public void setSiteUrl(String siteUrl) {
    this.siteUrl = siteUrl;
  }

  /** 화면 주소(예: {@code /account-info/opinions/1})를 메일에 넣을 전체 주소로 만든다. */
  public String linkTo(String path) {
    String base = siteUrl == null ? "" : siteUrl.strip();
    return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
  }
}
