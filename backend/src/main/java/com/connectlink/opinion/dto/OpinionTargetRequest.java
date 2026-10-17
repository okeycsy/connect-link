package com.connectlink.opinion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * 대상 기관 한 곳.
 *
 * @param formName 이 기관이 쓸 회신 양식의 이름. 양식 없이 회신하면 null
 * @param recipientAccountIds 회신 담당자(그 기관의 같은 업무 담당자 계정). 없어도 되지만 그러면 알림 메일이 가지 않는다
 */
public record OpinionTargetRequest(
    @NotNull(message = "대상 기관을 골라 주세요.") Long organizationId,
    @Schema(nullable = true) String formName,
    List<Long> recipientAccountIds) {

  public OpinionTargetRequest {
    recipientAccountIds =
        recipientAccountIds == null ? List.of() : List.copyOf(recipientAccountIds);
  }
}
