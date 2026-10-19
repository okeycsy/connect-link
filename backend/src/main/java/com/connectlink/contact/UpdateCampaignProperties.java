package com.connectlink.contact;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 정기 변경(현행화) 주간의 주기와 리마인드 설정 ({@code app.contact.campaign.*}). 명세는 "주기 6개월" 만 정해 나머지는 기본값을 두었다
 * (docs/decisions.md).
 *
 * <ul>
 *   <li>{@code auto-create}: 주기가 돌아오면 주간을 스스로 등록할지. 기본 참.
 *   <li>{@code cycle-months}: 정기 변경 주기. 그 업무 최근 주간 시작일에서 이만큼 지나면 새 주간을 등록한다. 기본 6개월.
 *   <li>{@code duration-days}: 스스로 등록하는 주간의 길이(시작일 포함). 기본 14일.
 *   <li>{@code reminder-interval-days}: 미회신 기관에 리마인드를 다시 보내는 간격. 요청 메일이나 앞 리마인드에서 이만큼 지나면 보낸다. 기본
 *       3일.
 *   <li>{@code cron}: 자동 등록과 리마인드를 살피는 때. 기본 매일 9시 30분(서버 시간대).
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.contact.campaign")
public class UpdateCampaignProperties {

  private boolean autoCreate = true;
  private int cycleMonths = 6;
  private int durationDays = 14;
  private int reminderIntervalDays = 3;
  private String cron = "0 30 9 * * *";

  public boolean isAutoCreate() {
    return autoCreate;
  }

  public void setAutoCreate(boolean autoCreate) {
    this.autoCreate = autoCreate;
  }

  public int getCycleMonths() {
    return cycleMonths;
  }

  public void setCycleMonths(int cycleMonths) {
    this.cycleMonths = cycleMonths;
  }

  public int getDurationDays() {
    return durationDays;
  }

  public void setDurationDays(int durationDays) {
    this.durationDays = durationDays;
  }

  public int getReminderIntervalDays() {
    return reminderIntervalDays;
  }

  public void setReminderIntervalDays(int reminderIntervalDays) {
    this.reminderIntervalDays = reminderIntervalDays;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }
}
