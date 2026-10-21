package com.connectlink.chat;

import com.connectlink.common.Labeled;

/** 채팅방 종류. 업무 단체방은 업무마다 하나씩 저절로 있고, 그 밖의 방은 결제원 담당자가 만든다. */
public enum ChatRoomType implements Labeled {
  BUSINESS("업무 단체방"),
  CUSTOM("초대방");

  private final String label;

  ChatRoomType(String label) {
    this.label = label;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
