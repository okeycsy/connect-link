package com.connectlink.contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 6개월 미확인 담당자 알림 예약 작업. 때는 {@code app.contact.reconfirm-cron}(기본 매일 9시, 서버 시간대)이다. */
@Component
class ContactReconfirmJob {

  private static final Logger log = LoggerFactory.getLogger(ContactReconfirmJob.class);

  private final ContactService contactService;

  ContactReconfirmJob(ContactService contactService) {
    this.contactService = contactService;
  }

  @Scheduled(cron = "${app.contact.reconfirm-cron:0 0 9 * * *}", zone = "Asia/Seoul")
  void run() {
    int sent = contactService.notifyReconfirmDue();
    if (sent > 0) {
      log.info("최근 확인일 6개월이 지난 담당자 {}명에게 확인 요청을 보냈습니다.", sent);
    }
  }
}
