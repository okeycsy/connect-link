package com.connectlink.common.demo;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 시연 사이트의 샘플 데이터를 매일 새벽에 오늘 기준으로 다시 넣는 예약 작업. 때는 {@code app.demo.sample-refresh-cron}(기본 매일 4시)이다.
 * 시연 사이트가 몇 주씩 켜져 있어도 회신 기한·테스트 일정 같은 날짜가 늘 앞에 있게 한다(SHY 요청, 2026-11-14). demo 프로필에만 있다.
 */
@Component
@Profile("demo")
class SampleDataRefreshJob {

  private final SampleDataRefresher refresher;

  SampleDataRefreshJob(SampleDataRefresher refresher) {
    this.refresher = refresher;
  }

  @Scheduled(cron = "${app.demo.sample-refresh-cron:0 0 4 * * *}", zone = "Asia/Seoul")
  void run() {
    refresher.refresh();
  }
}
