package com.connectlink.suspension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 종료 시각이 지난 확정 일시중단을 완료로 바꾸는 예약 작업. 1분마다 돈다. */
@Component
class SuspensionCompletionJob {

  private static final Logger log = LoggerFactory.getLogger(SuspensionCompletionJob.class);

  private final SuspensionService suspensionService;

  SuspensionCompletionJob(SuspensionService suspensionService) {
    this.suspensionService = suspensionService;
  }

  @Scheduled(fixedDelayString = "${app.suspension.completion-interval:60000}", initialDelay = 5000)
  void run() {
    int completed = suspensionService.completeEnded();
    if (completed > 0) {
      log.info("일시중단 {}건을 완료로 바꿨습니다.", completed);
    }
  }
}
