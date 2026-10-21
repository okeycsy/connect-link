package com.connectlink.testplan;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.testplan.dto.TestDataResponse;
import com.connectlink.testplan.dto.TestDataSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기관 테스트 전 테스트데이터 준비: 대상 기관별 생성 여부·메모. 파일은 첨부 API(ownerType TEST_DATA, ownerId = targetId)로 올린다.
 */
@RestController
@RequestMapping("/api/v1/test-plans/{id}/targets/{organizationId}/test-data")
@Tag(name = "기관 테스트데이터", description = "테스트 전 대상 기관별 테스트데이터 생성 여부와 파일")
public class TestDataController {

  private final TestDataService testDataService;
  private final CurrentUserProvider currentUserProvider;

  public TestDataController(
      TestDataService testDataService, CurrentUserProvider currentUserProvider) {
    this.testDataService = testDataService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "대상 기관 테스트데이터 조회",
      description = "한 대상 기관의 테스트데이터 생성 여부·메모·파일 수. 결제원 담당자는 어느 기관이든, 기관 담당자는 자기 기관만 본다.")
  public TestDataResponse get(@PathVariable Long id, @PathVariable Long organizationId) {
    return testDataService.get(currentUserProvider.require(), id, organizationId);
  }

  @PutMapping
  @Operation(
      summary = "대상 기관 테스트데이터 저장",
      description =
          "그 기관 담당자가 테스트데이터 생성 여부(미생성·생성 완료)와 메모를 저장한다. 생성 완료로 바뀌면 테스트의 알림 대상 결제원 담당자에게 매터모스트 알림이 간다.")
  public TestDataResponse save(
      @PathVariable Long id,
      @PathVariable Long organizationId,
      @Valid @RequestBody TestDataSaveRequest request) {
    return testDataService.save(currentUserProvider.require(), id, organizationId, request);
  }
}
