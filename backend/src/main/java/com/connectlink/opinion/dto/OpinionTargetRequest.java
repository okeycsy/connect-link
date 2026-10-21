package com.connectlink.opinion.dto;

import java.util.List;
import java.util.Locale;
import javax.validation.constraints.NotNull;

/**
 * 대상 기관 한 곳.
 *
 * @param recipientEmails 회신 담당자(그 기관의 같은 업무 담당자 연락처의 회사 이메일). 없어도 되지만 그러면 알림 메일이 가지 않는다
 */
public record OpinionTargetRequest(
    @NotNull(message = "대상 기관을 골라 주세요.") Long organizationId, List<String> recipientEmails) {

  public OpinionTargetRequest {
    recipientEmails =
        recipientEmails == null
            ? List.of()
            : recipientEmails.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
  }
}
