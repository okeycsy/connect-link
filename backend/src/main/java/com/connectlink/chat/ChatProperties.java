package com.connectlink.chat;

import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 소통 채널 설정 ({@code app.chat.*}).
 *
 * <ul>
 *   <li>{@code attachment-retention}: 첨부를 서버에 두는 기간. 지나면 매일 정리 작업이 지운다(명세 "1년 뒤 서버에서 삭제"). 기본 1년.
 *   <li>{@code attachment-cleanup-cron}: 정리 작업 때. 기본 매일 3시 30분(서버 시간대).
 * </ul>
 *
 * 파일 하나의 크기 한도는 첨부 공통 설정 {@code app.attachment.max-size}(기본 20MB)를 따른다.
 */
@Component
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

  private Period attachmentRetention = Period.ofYears(1);
  private String attachmentCleanupCron = "0 30 3 * * *";

  public Period getAttachmentRetention() {
    return attachmentRetention;
  }

  public void setAttachmentRetention(Period attachmentRetention) {
    this.attachmentRetention = attachmentRetention;
  }

  public String getAttachmentCleanupCron() {
    return attachmentCleanupCron;
  }

  public void setAttachmentCleanupCron(String attachmentCleanupCron) {
    this.attachmentCleanupCron = attachmentCleanupCron;
  }
}
