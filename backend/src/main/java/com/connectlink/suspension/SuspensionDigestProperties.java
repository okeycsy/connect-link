package com.connectlink.suspension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 전 영업일 취합 전달 설정 ({@code app.suspension.digest.*}). 명세에 없어 기본값을 두었다(docs/decisions.md).
 *
 * <ul>
 *   <li>{@code cron}: 보내는 때. 기본은 평일 17시. 휴일로 적힌 날은 건너뛴다.
 *   <li>{@code channels}: 받는 매터모스트 채널(운영팀, 관제팀). 운영에서 실제 채널 이름으로 바꾼다.
 *   <li>{@code holidays}: 주말 말고 쉬는 날(공휴일 등). 영업일 계산에 쓴다.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.suspension.digest")
public class SuspensionDigestProperties {

  private String cron = "0 0 17 * * MON-FRI";
  private List<String> channels = new ArrayList<>(List.of("~kftc-ops", "~kftc-control-room"));
  private List<LocalDate> holidays = new ArrayList<>();

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public List<String> getChannels() {
    return channels;
  }

  public void setChannels(List<String> channels) {
    this.channels = channels;
  }

  public List<LocalDate> getHolidays() {
    return holidays;
  }

  public void setHolidays(List<LocalDate> holidays) {
    this.holidays = holidays;
  }
}
