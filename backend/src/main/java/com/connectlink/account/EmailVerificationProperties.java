package com.connectlink.account;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 회사 이메일 인증 설정 ({@code app.account.verification.*}). 명세가 정하지 않아 기본값을 두었다(docs/decisions.md).
 *
 * <ul>
 *   <li>{@code code-ttl}: 인증 코드 유효 시간. 기본 10분.
 *   <li>{@code max-attempts}: 한 코드에 틀릴 수 있는 횟수. 넘으면 코드를 다시 받아야 한다. 기본 5번.
 *   <li>{@code resend-interval}: 코드를 다시 받기까지 기다리는 시간. 기본 60초.
 *   <li>{@code token-ttl}: 코드를 확인한 뒤 가입 신청을 마칠 수 있는 시간. 기본 30분.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.account.verification")
public class EmailVerificationProperties {

  private Duration codeTtl = Duration.ofMinutes(10);
  private int maxAttempts = 5;
  private Duration resendInterval = Duration.ofSeconds(60);
  private Duration tokenTtl = Duration.ofMinutes(30);

  public Duration getCodeTtl() {
    return codeTtl;
  }

  public void setCodeTtl(Duration codeTtl) {
    this.codeTtl = codeTtl;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public Duration getResendInterval() {
    return resendInterval;
  }

  public void setResendInterval(Duration resendInterval) {
    this.resendInterval = resendInterval;
  }

  public Duration getTokenTtl() {
    return tokenTtl;
  }

  public void setTokenTtl(Duration tokenTtl) {
    this.tokenTtl = tokenTtl;
  }
}
