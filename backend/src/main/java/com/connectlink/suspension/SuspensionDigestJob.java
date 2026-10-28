package com.connectlink.suspension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 일시중단 취합 예약 작업. 전 영업일 취합은 {@code app.suspension.digest.cron}(기본 평일 16시), 주간 취합은 {@code
 * app.suspension.digest.weekly-cron}(기본 평일 15시, 보내는 날인지는 영업일 달력이 정한다) 때 돈다. 서버 시간대다.
 */
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

  @Scheduled(cron = "${app.suspension.digest.weekly-cron:0 0 15 * * MON-FRI}", zone = "Asia/Seoul")
  void runWeekly() {
    digestService
        .sendWeeklyScheduled()
        .forEach(
            d ->
                log.info(
                    "이번 주 확정 일시중단 {}건을 {} 에 보냈습니다({}~{}).",
                    d.itemCount(),
                    d.recipients(),
                    d.fromDate(),
                    d.toDate()));
  }
}
