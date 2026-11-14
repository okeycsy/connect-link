package com.connectlink.chat;

import com.connectlink.common.demo.SampleDataRefreshedEvent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 서버가 뜰 때 업무마다 단체방이 하나씩 있게 한다(명세 "업무 기준 단체 채팅방이 자동으로 만들어지는 구조"). 이미 있으면 아무것도 하지 않는다. 샘플 데이터를 다시 넣은
 * 뒤({@link SampleDataRefreshedEvent})에도 같다.
 */
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

  @EventListener(SampleDataRefreshedEvent.class)
  public void onSampleDataRefreshed() {
    chatService.ensureBusinessRooms();
  }
}
