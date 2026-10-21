package com.connectlink.chat.dto;

/** "확인" 공감 하나. 화면은 기관 이름으로 묶어 "KB국민은행 확인" 꼴로 보인다. */
public record ChatAckResponse(
    String organizationCode, String organizationName, String name, boolean mine) {}
