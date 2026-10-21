package com.connectlink.chat.dto;

import java.util.List;
import java.util.Locale;
import javax.validation.constraints.NotEmpty;

/** 방에 사람 초대하기(회사 이메일). */
public record ChatInviteRequest(@NotEmpty(message = "초대할 사람을 골라 주세요.") List<String> emails) {

  public ChatInviteRequest {
    emails = normalize(emails);
  }

  /** 빈 값을 빼고 소문자로 맞춰 겹치지 않게 한다. */
  static List<String> normalize(List<String> emails) {
    return emails == null
        ? List.of()
        : emails.stream()
            .filter(e -> e != null && !e.isBlank())
            .map(e -> e.trim().toLowerCase(Locale.ROOT))
            .distinct()
            .toList();
  }
}
