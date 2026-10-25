package com.connectlink.testplan.dto;

import com.connectlink.organization.FinanceSector;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 대상 기관 후보 한 곳. 그 업무에 참여하는 사용 중 참가기관이다. 업권으로 묶어 고른다.
 *
 * @param members 테스트 담당자로 고를 수 있는 사람(그 기관·업무의 담당자 연락처, 계정이 없는 후임자 포함). 세부업무를 보고 고르고, 회사 이메일로 테스트에
 *     넣는다
 */
public record TestPlanOrganizationResponse(
    Long id,
    String code,
    String name,
    @Schema(nullable = true) FinanceSector sector,
    @Schema(nullable = true) String sectorLabel,
    List<Member> members) {

  @Schema(name = "TestPlanCandidateMember")
  public record Member(
      Long contactId,
      String name,
      String email,
      @Schema(nullable = true) String subBusiness,
      String jobRoleLabel,
      String department,
      String team,
      String jobTitle) {}
}
