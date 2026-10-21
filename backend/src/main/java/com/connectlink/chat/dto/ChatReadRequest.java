package com.connectlink.chat.dto;

import javax.validation.constraints.NotNull;

/** 이 메시지까지 읽었다. */
public record ChatReadRequest(@NotNull(message = "읽은 메시지를 알려 주세요.") Long lastMessageId) {}
