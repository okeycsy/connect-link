package com.connectlink.chat;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.chat.dto.ChatMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 소통 채널 메시지의 "확인" 공감. */
@RestController
@RequestMapping("/api/v1/chat-messages")
@Tag(name = "소통 채널", description = "업무 단체방과 초대방, 메시지, 멘션, 확인 공감")
public class ChatMessageController {

  private final ChatService chatService;
  private final CurrentUserProvider currentUserProvider;

  public ChatMessageController(ChatService chatService, CurrentUserProvider currentUserProvider) {
    this.chatService = chatService;
    this.currentUserProvider = currentUserProvider;
  }

  @PutMapping("/{id}/ack")
  @Operation(
      summary = "확인 공감 남기기",
      description = "답장 대신 확인했다고 남긴다. 메시지 아래에 기관 이름으로 보인다. 내가 보낸 메시지면 409 CHAT_ACK_OWN_MESSAGE.")
  public ChatMessageResponse acknowledge(@PathVariable Long id) {
    return chatService.acknowledge(currentUserProvider.require(), id);
  }

  @DeleteMapping("/{id}/ack")
  @Operation(summary = "확인 공감 거두기", description = "내가 남긴 확인 공감을 거둔다.")
  public ChatMessageResponse withdrawAck(@PathVariable Long id) {
    return chatService.withdrawAck(currentUserProvider.require(), id);
  }
}
