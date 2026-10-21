package com.connectlink.chat.dto;

/** 멘션할 수 있는(또는 멘션된) 기관 하나. 결제원은 code KFTC, label "결제원" 이다. */
public record ChatMentionResponse(String organizationCode, String label) {}
