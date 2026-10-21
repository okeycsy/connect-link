package com.connectlink.chat.demo;

import com.connectlink.chat.ChatService;
import com.connectlink.chat.dto.ChatAttachmentCleanupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 시연용: 채팅 첨부 보관 기간 정리 예약 작업을 지금 한 번 돌린다(CLAUDE.md 6절 "예약 작업"). local·demo·test 프로필에만 있다. */
@RestController
@RequestMapping("/api/v1/demo/chat-attachment-cleanup-runs")
@Profile({"local", "demo", "test"})
@Tag(name = "데모 도구", description = "시연용 도구 (demo·local 프로필 전용)")
public class ChatDemoController {

  private final ChatService chatService;

  public ChatDemoController(ChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping
  @Operation(
      summary = "채팅 첨부 보관 기간 정리 지금 돌리기(데모)",
      description = "매일 3시 30분 예약 작업과 같다. 보관 기간(기본 1년)이 지난 메시지의 첨부를 지우고 메시지에 표시한다.")
  public ChatAttachmentCleanupResponse run() {
    return new ChatAttachmentCleanupResponse(chatService.expireAttachments());
  }
}
