package com.connectlink.chat.dto;

/** 보관 기간이 지난 첨부 정리 결과. expiredMessages 는 첨부를 지운 메시지 수다. */
public record ChatAttachmentCleanupResponse(int expiredMessages) {}
