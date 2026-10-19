package com.connectlink.suspension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 전 영업일 취합 전달 예약 작업. 때는 {@code app.suspension.digest.cron}(기본 평일 16시, 서버 시간대)이다. */
@Component
class SuspensionDigestJob {

  private static final Logger log = LoggerFactory.getLogger(SuspensionDigestJob.class);

  private final SuspensionDigestService digestService;

  SuspensionDigestJob(SuspensionDigestService digestService) {
    this.digestService = digestService;
  }

  @Scheduled(cron = "${app.suspension.digest.cron:0 0 16 * * MON-FRI}", zone = "Asia/Seoul")
  void run() {
    digestService
        .sendScheduled()
        .ifPresent(
            d ->
                log.info(
                    "일시중단 취합 {}건을 {} 에 보냈습니다({}~{}).",
                    d.itemCount(),
                    d.recipients(),
                    d.fromDate(),
                    d.toDate()));
  }
}
