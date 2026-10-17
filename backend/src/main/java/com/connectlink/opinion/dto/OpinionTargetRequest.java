package com.connectlink.opinion.dto;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * 대상 기관 한 곳.
 *
 * @param recipientAccountIds 회신 담당자(그 기관의 같은 업무 담당자 계정). 없어도 되지만 그러면 알림 메일이 가지 않는다
 */
public record OpinionTargetRequest(
    @NotNull(message = "대상 기관을 골라 주세요.") Long organizationId, List<Long> recipientAccountIds) {

  public OpinionTargetRequest {
    recipientAccountIds =
        recipientAccountIds == null ? List.of() : List.copyOf(recipientAccountIds);
  }
}
