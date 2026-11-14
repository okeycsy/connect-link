package com.connectlink.account;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 로그인 실패 잠금 설정 ({@code app.account.login.*}). 개인정보의 안전성 확보조치 기준(개인정보보호위원회 고시) 제5조의 "일정 횟수 이상 인증에
 * 실패하면 접근 제한" 을 따른다. 값은 docs/decisions.md.
 *
 * <ul>
 *   <li>{@code max-failures}: 잠그기까지 연달아 틀릴 수 있는 횟수. 기본 5번.
 *   <li>{@code lock-duration}: 잠그는 시간. 기본 30분. 비밀번호 찾기로 새 비밀번호를 정하면 바로 풀린다.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.account.login")
public class LoginLockProperties {

  private int maxFailures = 5;
  private Duration lockDuration = Duration.ofMinutes(30);

  public int getMaxFailures() {
    return maxFailures;
  }

  public void setMaxFailures(int maxFailures) {
    this.maxFailures = maxFailures;
  }

  public Duration getLockDuration() {
    return lockDuration;
  }

  public void setLockDuration(Duration lockDuration) {
    this.lockDuration = lockDuration;
  }
}
