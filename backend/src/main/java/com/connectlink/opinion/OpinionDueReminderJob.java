package com.connectlink.opinion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 의견 회신 기한 하루 전 자동 리마인드 예약 작업. 때는 {@code app.opinion.due-reminder-cron}(기본 매일 9시, 서버 시간대)이다. */
@Component
class OpinionDueReminderJob {

  private static final Logger log = LoggerFactory.getLogger(OpinionDueReminderJob.class);

  private final OpinionCollationService collationService;

  OpinionDueReminderJob(OpinionCollationService collationService) {
    this.collationService = collationService;
  }

  @Scheduled(cron = "${app.opinion.due-reminder-cron:0 0 9 * * *}", zone = "Asia/Seoul")
  void run() {
    int sent = collationService.remindDueTomorrow();
    if (sent > 0) {
      log.info("회신 기한이 내일인 의견 요청 {}건의 미회신 기관에 리마인드를 보냈습니다.", sent);
    }
  }
}
