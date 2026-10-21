package com.connectlink.testplan.dto;

import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 기관 테스트 만들기·고치기 양식. 업무는 만든 뒤 바꾸지 않는다. 가이드·참고자료(TEST_PLAN)는 저장한 뒤 첨부 API 로 올린다.
 *
 * @param subBusiness 단위업무
 * @param title 개발 건 이름(테스트명)
 * @param description 테스트 내용·수행 가이드
 * @param items 요구사항(정한 순서). 하나 이상
 * @param organizationIds 대상 참가기관. 그 업무에 참여하는 사용 중 참가기관, 하나 이상
 * @param testDataRequest 수행 기간 전에 받을 테스트데이터 요청. 받지 않으면 null
 */
public record TestPlanSaveRequest(
    @NotNull(message = "업무를 골라 주세요.") Business business,
    @NotBlank(message = "단위업무를 입력해 주세요.") @Size(max = 200, message = "단위업무는 200자까지입니다.")
        String subBusiness,
    @NotBlank(message = "개발 건 이름을 입력해 주세요.") @Size(max = 200, message = "개발 건 이름은 200자까지입니다.")
        String title,
    @Schema(nullable = true) @Size(max = 4000, message = "테스트 내용은 4000자까지입니다.") String description,
    @NotNull(message = "수행 시작일을 골라 주세요.") LocalDate startDate,
    @NotNull(message = "수행 종료일을 골라 주세요.") LocalDate endDate,
    @Valid @Size(max = 200, message = "요구사항은 200개까지입니다.") List<TestItemRequest> items,
    List<Long> organizationIds,
    @Valid @Schema(nullable = true) TestDataRequestForm testDataRequest) {

  public TestPlanSaveRequest {
    items = items == null ? List.of() : List.copyOf(items);
    organizationIds = organizationIds == null ? List.of() : List.copyOf(organizationIds);
  }
}
