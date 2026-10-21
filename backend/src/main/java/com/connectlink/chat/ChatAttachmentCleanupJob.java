package com.connectlink.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 보관 기간(기본 1년)이 지난 채팅 첨부를 지우는 예약 작업. 때는 {@code app.chat.attachment-cleanup-cron}(기본 매일 3시 30분)이다.
 */
@Component
class ChatAttachmentCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(ChatAttachmentCleanupJob.class);

  private final ChatService chatService;

  ChatAttachmentCleanupJob(ChatService chatService) {
    this.chatService = chatService;
  }

  @Scheduled(cron = "${app.chat.attachment-cleanup-cron:0 30 3 * * *}", zone = "Asia/Seoul")
  void run() {
    int expired = chatService.expireAttachments();
    if (expired > 0) {
      log.info("보관 기간이 지난 채팅 메시지 {}건의 첨부를 지웠습니다.", expired);
    }
  }
}
