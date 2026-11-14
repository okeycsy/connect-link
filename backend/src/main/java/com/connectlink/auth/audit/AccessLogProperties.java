package com.connectlink.auth.audit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 접속기록 설정 ({@code app.audit.access-log.*}).
 *
 * <ul>
 *   <li>{@code retention}: 보관 기간. 개인정보의 안전성 확보조치 기준 제8조는 1년 이상(정보주체 5만 명 이상 등은 2년 이상)을 요구한다. 기본
 *       2년(730일)으로 두 경우를 모두 넘긴다.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.audit.access-log")
public class AccessLogProperties {

  private Duration retention = Duration.ofDays(730);

  public Duration getRetention() {
    return retention;
  }

  public void setRetention(Duration retention) {
    this.retention = retention;
  }
}
