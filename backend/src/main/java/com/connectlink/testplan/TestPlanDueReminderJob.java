package com.connectlink.testplan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 기관 테스트 기한 하루 전 자동 리마인드 예약 작업(수행 종료일, 테스트데이터 회신 기한). 때는 {@code app.testplan.due-reminder-cron}(기본
 * 매일 9시, 서버 시간대)이다.
 */
@Component
class TestPlanDueReminderJob {

  private static final Logger log = LoggerFactory.getLogger(TestPlanDueReminderJob.class);

  private final TestPlanService testPlanService;

  TestPlanDueReminderJob(TestPlanService testPlanService) {
    this.testPlanService = testPlanService;
  }

  @Scheduled(cron = "${app.testplan.due-reminder-cron:0 0 9 * * *}", zone = "Asia/Seoul")
  void run() {
    int sent = testPlanService.remindDueTomorrow();
    if (sent > 0) {
      log.info("기한이 내일인 기관 테스트에서 기관 {}곳에 리마인드를 보냈습니다.", sent);
    }
  }
}
