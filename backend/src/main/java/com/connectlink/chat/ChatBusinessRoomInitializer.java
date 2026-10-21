package com.connectlink.chat;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 서버가 뜰 때 업무마다 단체방이 하나씩 있게 한다(명세 "업무 기준 단체 채팅방이 자동으로 만들어지는 구조"). 이미 있으면 아무것도 하지 않는다. */
@Component
class ChatBusinessRoomInitializer implements ApplicationRunner {

  private final ChatService chatService;

  ChatBusinessRoomInitializer(ChatService chatService) {
    this.chatService = chatService;
  }

  @Override
  public void run(ApplicationArguments args) {
    chatService.ensureBusinessRooms();
  }
}
