package com.connectlink.auth.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 새벽 보관 기간이 지난 접속기록을 지운다. */
@Component
class AccessLogCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(AccessLogCleanupJob.class);

  private final AccessLogService service;

  AccessLogCleanupJob(AccessLogService service) {
    this.service = service;
  }

  @Scheduled(cron = "${app.audit.access-log.cleanup-cron:0 30 3 * * *}", zone = "Asia/Seoul")
  void run() {
    int deleted = service.deleteExpired();
    if (deleted > 0) {
      log.info("보관 기간이 지난 접속기록 {}건을 지웠습니다.", deleted);
    }
  }
}
