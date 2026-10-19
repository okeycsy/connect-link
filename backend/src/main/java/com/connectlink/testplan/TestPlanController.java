package com.connectlink.testplan;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.organization.Business;
import com.connectlink.testplan.dto.TestPlanCountsResponse;
import com.connectlink.testplan.dto.TestPlanDetailResponse;
import com.connectlink.testplan.dto.TestPlanOrganizationResponse;
import com.connectlink.testplan.dto.TestPlanRecipientResponse;
import com.connectlink.testplan.dto.TestPlanRecipientsRequest;
import com.connectlink.testplan.dto.TestPlanSaveRequest;
import com.connectlink.testplan.dto.TestPlanSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기관 테스트: 진행 중·수행 완료 목록, 결제원 담당자의 만들기·고치기·지우기와 결과 등록 알림 대상 설정. */
@RestController
@RequestMapping("/api/v1/test-plans")
@Tag(name = "기관 테스트", description = "기관 테스트 수행·결과 관리 페이지와 알림 대상")
public class TestPlanController {

  private final TestPlanService testPlanService;
  private final CurrentUserProvider currentUserProvider;

  public TestPlanController(
      TestPlanService testPlanService, CurrentUserProvider currentUserProvider) {
    this.testPlanService = testPlanService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "기관 테스트 목록 조회",
      description =
          "한 업무 영역의 기관 테스트. state 가 없으면 전부(최근 만든 순)다. 결제원 담당자는 전부와 전체 수행률을, 기관 담당자는 자기 기관이 대상인 것과 자기 기관 수행률을 본다.")
  public PageResponse<TestPlanSummaryResponse> list(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @Parameter(description = "IN_PROGRESS(진행 중), COMPLETED(수행 완료). 없으면 전부")
          @RequestParam(required = false)
          TestPlanState state,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return testPlanService.list(
        currentUserProvider.require(),
        resolveBusiness(business),
        state,
        PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/counts")
  @Operation(summary = "기관 테스트 진행 구분별 건수 조회", description = "목록의 필터 칩이 쓴다.")
  public TestPlanCountsResponse counts(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return testPlanService.counts(currentUserProvider.require(), resolveBusiness(business));
  }

  @GetMapping("/organizations")
  @Operation(
      summary = "기관 테스트 대상 기관 후보 조회",
      description = "그 업무에 참여하는 사용 중 참가기관(업권 포함). 그 업무 결제원 담당자만 쓴다.")
  public List<TestPlanOrganizationResponse> organizations(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return testPlanService.organizations(currentUserProvider.require(), resolveBusiness(business));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "기관 테스트 만들기",
      description =
          "그 업무 결제원 담당자가 단위업무/개발 건마다 만든다. 요구사항과 대상 기관이 하나 이상 있어야 한다(TEST_PLAN_ITEMS_REQUIRED, TEST_PLAN_TARGETS_REQUIRED). 바로 대상 기관에 열리고 대상 기관 담당자에게 알림·메일이 간다. 만든 사람이 결과 등록 알림 대상이 된다. 가이드·참고자료(TEST_PLAN)는 저장한 뒤 첨부 API 로 올린다.")
  public TestPlanDetailResponse create(@Valid @RequestBody TestPlanSaveRequest request) {
    return testPlanService.create(currentUserProvider.require(), request);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "기관 테스트 상세 조회",
      description = "기관 담당자에게는 자기 기관 현황만 준다. permissions 가 지금 할 수 있는 일이다.")
  public TestPlanDetailResponse get(@PathVariable Long id) {
    return testPlanService.get(currentUserProvider.require(), id);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "기관 테스트 고치기",
      description =
          "그 업무 결제원 담당자가 내용·요구사항·대상 기관을 고친다. 업무는 바꿀 수 없다(TEST_PLAN_BUSINESS_FIXED). 결과가 있는 요구사항·기관을 빼면 409 TEST_ITEM_HAS_RESULTS, TEST_TARGET_HAS_RESULTS. 새로 더한 대상 기관 담당자에게 알림·메일이 간다.")
  public TestPlanDetailResponse update(
      @PathVariable Long id, @Valid @RequestBody TestPlanSaveRequest request) {
    return testPlanService.update(currentUserProvider.require(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "기관 테스트 지우기", description = "그 업무 결제원 담당자가 지운다. 기관 결과와 가이드 첨부도 함께 지운다.")
  public void delete(@PathVariable Long id) {
    testPlanService.delete(currentUserProvider.require(), id);
  }

  @GetMapping("/{id}/recipients")
  @Operation(
      summary = "결과 등록 알림 대상 조회",
      description = "그 업무를 맡은 사용 중 결제원 담당자 후보와 지금 고른 사람(selected). 그 업무 결제원 담당자만 쓴다.")
  public List<TestPlanRecipientResponse> recipients(@PathVariable Long id) {
    return testPlanService.recipients(currentUserProvider.require(), id);
  }

  @PutMapping("/{id}/recipients")
  @Operation(
      summary = "결과 등록 알림 대상 바꾸기",
      description =
          "기관이 결과를 등록하면 매터모스트 알림을 받을 결제원 담당자를 통째로 바꾼다. 후보가 아닌 계정이면 400 TEST_RECIPIENT_INVALID.")
  public List<TestPlanRecipientResponse> replaceRecipients(
      @PathVariable Long id, @Valid @RequestBody TestPlanRecipientsRequest request) {
    return testPlanService.replaceRecipients(
        currentUserProvider.require(), id, request.accountIds());
  }

  static Business resolveBusiness(String slug) {
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
