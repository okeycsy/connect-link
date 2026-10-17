package com.connectlink.opinion.dto;

import com.connectlink.organization.FinanceSector;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 대상으로 고를 수 있는 기관 한 곳과 그 기관의 같은 업무 담당자들. 요청 작성 화면이 대상 기관과 회신 담당자를 고를 때 쓴다.
 *
 * @param sector 업권. 업권별로 대상 기관을 한꺼번에 고를 때 쓴다. 정하지 않은 기관은 null
 * @param members 회신 담당자로 고를 수 있는 사람(사용 중 계정). 세부업무를 보고 고른다
 */
public record OpinionRecipientCandidateResponse(
    Long organizationId,
    String organizationCode,
    String organizationName,
    @Schema(nullable = true) FinanceSector sector,
    @Schema(nullable = true) String sectorLabel,
    List<Member> members) {

  @Schema(name = "OpinionRecipientCandidateMember")
  public record Member(
      Long accountId,
      String name,
      String email,
      @Schema(nullable = true) String subBusiness,
      String jobRoleLabel,
      String department,
      String team,
      String jobTitle) {}
}
