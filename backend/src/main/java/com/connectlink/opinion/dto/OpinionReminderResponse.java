package com.connectlink.opinion.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 리마인드 메일 발송 이력 한 건. */
public record OpinionReminderResponse(
    Long id,
    LocalDateTime sentAt,
    String senderName,
    int organizationCount,
    int recipientCount,
    List<String> organizationNames) {}
