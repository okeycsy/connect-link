package com.connectlink.testplan;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.testplan.dto.TestFeedbackRequest;
import com.connectlink.testplan.dto.TestItemCommentRequest;
import com.connectlink.testplan.dto.TestResultSaveRequest;
import com.connectlink.testplan.dto.TestTargetResultsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 기관 테스트 결과: 대상 기관별 요구사항 결과 조회와 등록, 결제원 의견(미니 수행률·특이사항)과 그 의견에 이어 다는 답글. */
@RestController
@RequestMapping("/api/v1/test-plans/{id}/targets/{organizationId}")
@Tag(name = "기관 테스트 결과", description = "대상 기관의 요구사항별 결과와 결제원 의견, 답글")
public class TestResultController {

  private final TestResultService testResultService;
  private final CurrentUserProvider currentUserProvider;

  public TestResultController(
      TestResultService testResultService, CurrentUserProvider currentUserProvider) {
    this.testResultService = testResultService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "대상 기관 결과 조회",
      description = "한 대상 기관의 요구사항별 결과와 결제원 의견. 결제원 담당자는 어느 기관이든, 기관 담당자는 자기 기관만 본다.")
  public TestTargetResultsResponse get(@PathVariable Long id, @PathVariable Long organizationId) {
    return testResultService.get(currentUserProvider.require(), id, organizationId);
  }

  @PutMapping("/results")
  @Operation(
      summary = "대상 기관 결과 저장",
      description =
          "그 기관 담당자가 요구사항별 결과(정상·오류·수행중·미수행)와 특이사항을 저장한다. 오류면 특이사항이 필요하다(TEST_RESULT_NOTE_REQUIRED). 바뀐 것이 있으면 테스트의 알림 대상 결제원 담당자에게 매터모스트 알림이 간다.")
  public TestTargetResultsResponse report(
      @PathVariable Long id,
      @PathVariable Long organizationId,
      @Valid @RequestBody TestResultSaveRequest request) {
    return testResultService.report(currentUserProvider.require(), id, organizationId, request);
  }

  @PutMapping("/items/{itemId}/feedback")
  @Operation(
      summary = "결제원 의견 저장",
      description =
          "그 업무 결제원 담당자가 기관 결과 하나에 미니 수행률(0~100)과 특이사항을 남긴다. 둘 다 비우면 지운다. 미니 수행률 100 이고 기관 결과가 정상이면 그 요구사항은 수행 완료다. 결과가 없는 요구사항이면 409 TEST_RESULT_NOT_REPORTED.")
  public TestTargetResultsResponse feedback(
      @PathVariable Long id,
      @PathVariable Long organizationId,
      @PathVariable Long itemId,
      @Valid @RequestBody TestFeedbackRequest request) {
    return testResultService.feedback(
        currentUserProvider.require(), id, organizationId, itemId, request);
  }

  @PostMapping("/items/{itemId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "결제원 의견에 답글 달기",
      description =
          "그 업무 결제원 담당자와 그 기관 담당자가 기관 결과 하나의 결제원 의견 아래에 시간순으로 이어서 답글을 단다. 상대 쪽 담당자에게 알림 센터로 알린다. 결과가 없는 요구사항이면 409 TEST_RESULT_NOT_REPORTED.")
  public TestTargetResultsResponse comment(
      @PathVariable Long id,
      @PathVariable Long organizationId,
      @PathVariable Long itemId,
      @Valid @RequestBody TestItemCommentRequest request) {
    return testResultService.comment(
        currentUserProvider.require(), id, organizationId, itemId, request);
  }

  @DeleteMapping("/items/{itemId}/comments/{commentId}")
  @Operation(summary = "답글 지우기", description = "자기가 쓴 답글을 지운다. 다른 사람 답글이면 403.")
  public TestTargetResultsResponse deleteComment(
      @PathVariable Long id,
      @PathVariable Long organizationId,
      @PathVariable Long itemId,
      @PathVariable Long commentId) {
    return testResultService.deleteComment(
        currentUserProvider.require(), id, organizationId, itemId, commentId);
  }
}
