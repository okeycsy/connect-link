package com.connectlink.attachment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 보안 검사 중인 첨부파일의 결과를 짧은 간격으로 확인해 통과·차단으로 바꾼다. */
@Component
class AttachmentInspectionJob {

  private static final Logger log = LoggerFactory.getLogger(AttachmentInspectionJob.class);

  private final AttachmentInspectionService service;

  AttachmentInspectionJob(AttachmentInspectionService service) {
    this.service = service;
  }

  @Scheduled(
      fixedDelayString = "${app.attachment.inspection.poll-interval:1000}",
      initialDelayString = "${app.attachment.inspection.poll-interval:1000}")
  void run() {
    int completed = service.completePending();
    if (completed > 0) {
      log.info("첨부파일 {}건의 반입 보안 검사가 끝났습니다.", completed);
    }
  }
}
