package com.connectlink.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** 테스트용 시계. 처음에는 지금 시각이고 {@link #advance} 로 앞으로 돌린다. 유효 시간·재발송 간격 테스트가 쓴다. */
public class MutableClock extends Clock {

  private Instant instant = Instant.now();
  private final ZoneId zone;

  public MutableClock(ZoneId zone) {
    this.zone = zone;
  }

  public void advance(Duration duration) {
    instant = instant.plus(duration);
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    MutableClock clock = new MutableClock(zone);
    clock.instant = instant;
    return clock;
  }

  @Override
  public Instant instant() {
    return instant;
  }
}
