package com.connectlink.common.demo;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 샘플 데이터를 오늘 기준으로 다시 넣는다. 시드(db/seed)의 기한·일정은 넣는 날 기준 상대값이라, 서버가 며칠씩 켜져 있으면(시연 사이트는 잠들지 않게 깨운다) 회신
 * 기한·테스트 일정·일시중단 일정이 모두 지난 날짜가 된다. 메모리 DB 를 비우고 Flyway 로 스키마와 시드를 다시 넣은 뒤 {@link
 * SampleDataRefreshedEvent} 를 낸다. 시연 중 만든 데이터와 세션의 로그인 정보도 함께 사라진다.
 *
 * <p>샘플 데이터를 쓰는 프로필(local, demo, test)에만 있다. 운영 DB 에는 절대 쓰지 않는다.
 */
@Component
@Profile({"local", "demo", "test"})
public class SampleDataRefresher {

  private static final Logger log = LoggerFactory.getLogger(SampleDataRefresher.class);

  private final Flyway flyway;
  private final ApplicationEventPublisher events;

  public SampleDataRefresher(Flyway flyway, ApplicationEventPublisher events) {
    this.flyway = flyway;
    this.events = events;
  }

  /** 스키마를 비우고 마이그레이션과 시드를 처음부터 다시 돌린다. 동시에 두 번 돌지 않는다. */
  public synchronized void refresh() {
    Flyway cleanable =
        Flyway.configure().configuration(flyway.getConfiguration()).cleanDisabled(false).load();
    cleanable.clean();
    cleanable.migrate();
    events.publishEvent(new SampleDataRefreshedEvent());
    log.info("샘플 데이터를 오늘 기준으로 다시 넣었습니다.");
  }
}
