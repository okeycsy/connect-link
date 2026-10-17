package com.connectlink.opinion;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.opinion.dto.OpinionReasonRequest;
import com.connectlink.opinion.dto.OpinionRecipientCandidateResponse;
import com.connectlink.opinion.dto.OpinionRequestCountsResponse;
import com.connectlink.opinion.dto.OpinionRequestCreateRequest;
import com.connectlink.opinion.dto.OpinionRequestDetailResponse;
import com.connectlink.opinion.dto.OpinionRequestSummaryResponse;
import com.connectlink.opinion.dto.OpinionRequestUpdateRequest;
import com.connectlink.organization.Business;
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

/** 의견 요청: 목록·상세(결제원과 대상 기관), 작성·수정·회신 요청 보내기·마감·취소·삭제(결제원). */
@RestController
@RequestMapping("/api/v1/opinion-requests")
@Tag(name = "의견 요청", description = "결제원 담당자의 의견 요청 작성과 상태 관리")
public class OpinionRequestController {

  private final OpinionRequestService requestService;
  private final CurrentUserProvider currentUserProvider;

  public OpinionRequestController(
      OpinionRequestService requestService, CurrentUserProvider currentUserProvider) {
    this.requestService = requestService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "의견 요청 목록 조회",
      description =
          "한 업무 영역의 요청 중 한 상태. 결제원 담당자는 그 업무의 모든 요청을, 기관 담당자는 자기 기관이 대상인 회신중·마감 요청만 본다(작성중은 빈 목록).")
  public PageResponse<OpinionRequestSummaryResponse> list(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @RequestParam(defaultValue = "OPEN") OpinionRequestStatus status,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return requestService.list(
        currentUserProvider.require(),
        resolveBusiness(business),
        status,
        PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/counts")
  @Operation(summary = "의견 요청 상태별 건수 조회", description = "목록의 필터 칩이 쓴다. 기관 담당자의 작성중은 0 이다.")
  public OpinionRequestCountsResponse counts(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return requestService.counts(currentUserProvider.require(), resolveBusiness(business));
  }

  @GetMapping("/recipient-candidates")
  @Operation(
      summary = "대상 기관·회신 담당자 후보 조회",
      description = "그 업무에 참여하는 사용 중 참가기관과 기관마다 같은 업무 담당자(세부업무 포함). 그 업무의 결제원 담당자만 본다.")
  public List<OpinionRecipientCandidateResponse> candidates(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return requestService.candidates(currentUserProvider.require(), resolveBusiness(business));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "의견 요청 작성",
      description = "작성중으로 저장한다. 첨부자료(OPINION_REQUEST)는 저장한 뒤 첨부 API 로 올린다.")
  public OpinionRequestDetailResponse create(
      @Valid @RequestBody OpinionRequestCreateRequest request) {
    return requestService.create(currentUserProvider.require(), request);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "의견 요청 상세 조회",
      description = "결제원 담당자는 대상 전부를, 기관 담당자는 자기 기관의 대상 행과 양식만 본다. permissions 가 지금 할 수 있는 일이다.")
  public OpinionRequestDetailResponse get(@PathVariable Long id) {
    return requestService.get(currentUserProvider.require(), id);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "의견 요청 수정",
      description =
          "작성중·회신중에 고친다. 양식·대상은 보낸 목록으로 바뀐다. 회신중이면 새 회신 담당자에게 요청 메일, 남은 담당자에게 변경 알림이 간다. 회신을 저장한 기관을 빼면 409 OPINION_TARGET_HAS_REPLY.")
  public OpinionRequestDetailResponse update(
      @PathVariable Long id, @Valid @RequestBody OpinionRequestUpdateRequest request) {
    return requestService.update(currentUserProvider.require(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "작성중 의견 요청 삭제", description = "작성중만 지운다. 회신중이면 409 이고 취소를 쓴다.")
  public void delete(@PathVariable Long id) {
    requestService.delete(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/opening")
  @Operation(
      summary = "회신 요청 보내기",
      description = "작성중 요청을 회신중으로 열고 회신 담당자에게 알림과 메일을 보낸다. 대상이 없으면 400 OPINION_TARGETS_REQUIRED.")
  public OpinionRequestDetailResponse open(@PathVariable Long id) {
    return requestService.open(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/closing")
  @Operation(summary = "의견 요청 마감", description = "회신중 요청을 마감한다. 더는 회신을 받지 않는다.")
  public OpinionRequestDetailResponse close(@PathVariable Long id) {
    return requestService.close(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/cancellation")
  @Operation(
      summary = "의견 요청 취소",
      description = "회신중 요청을 사유와 함께 취소한다. 마감 상태가 되고 회신 담당자에게 알림과 메일이 간다.")
  public OpinionRequestDetailResponse cancel(
      @PathVariable Long id, @Valid @RequestBody OpinionReasonRequest request) {
    return requestService.cancel(currentUserProvider.require(), id, request.reason());
  }

  static Business resolveBusiness(String slug) {
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
