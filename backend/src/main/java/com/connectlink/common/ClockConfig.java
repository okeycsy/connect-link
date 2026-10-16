package com.connectlink.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 서버 시계. 유효 시간·재발송 간격처럼 시간에 기대는 규칙은 이 시계로 판단해 테스트에서 시간을 고정할 수 있게 한다. 서버 시간대는 Asia/Seoul. */
@Configuration
public class ClockConfig {

  public static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

  @Bean
  public Clock clock() {
    return Clock.system(SERVER_ZONE);
  }
}
