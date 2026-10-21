package com.connectlink.chat;

import com.connectlink.account.AccountWithdrawnEvent;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정이 탈퇴하면 초대받은 방에서 빼고 읽음 표시를 지운다. 업무 단체방은 사용 중 계정으로 구성원을 정하므로 저절로 빠진다. 탈퇴와 같은 트랜잭션이라 실패하면 탈퇴도
 * 되돌아간다.
 */
@Component
class ChatAccountListener {

  private final ChatService chatService;

  ChatAccountListener(ChatService chatService) {
    this.chatService = chatService;
  }

  @EventListener
  @Transactional
  public void onWithdrawn(AccountWithdrawnEvent event) {
    chatService.removeEverywhere(event.email().toLowerCase(Locale.ROOT));
  }
}
