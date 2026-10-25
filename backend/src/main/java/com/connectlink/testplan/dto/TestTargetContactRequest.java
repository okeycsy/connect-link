package com.connectlink.testplan.dto;

import java.util.List;
import java.util.Locale;
import javax.validation.constraints.NotNull;

/**
 * 대상 기관 한 곳의 테스트 담당자.
 *
 * @param organizationId 대상 기관. 양식의 organizationIds 에 들어 있어야 한다
 * @param emails 테스트 담당자(그 기관의 같은 업무 담당자 연락처의 회사 이메일). 비우면 그 기관의 업무 담당자 전원이 메일을 받는다
 */
public record TestTargetContactRequest(
    @NotNull(message = "대상 기관을 골라 주세요.") Long organizationId, List<String> emails) {

  public TestTargetContactRequest {
    emails =
        emails == null
            ? List.of()
            : emails.stream()
                .filter(e -> e != null && !e.isBlank())
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
  }
}
