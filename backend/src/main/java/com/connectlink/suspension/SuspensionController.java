package com.connectlink.suspension;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.organization.Business;
import com.connectlink.suspension.dto.SuspensionCountsResponse;
import com.connectlink.suspension.dto.SuspensionDetailResponse;
import com.connectlink.suspension.dto.SuspensionHandoverRequest;
import com.connectlink.suspension.dto.SuspensionOrganizationResponse;
import com.connectlink.suspension.dto.SuspensionRegistrableOrganizationResponse;
import com.connectlink.suspension.dto.SuspensionSaveRequest;
import com.connectlink.suspension.dto.SuspensionScheduleResponse;
import com.connectlink.suspension.dto.SuspensionSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 일시중단: 진행 중·예정·지난 목록과 기관별 이력, 등록·수정·삭제, 결제원 담당자의 확인 시작과 확정. */
@RestController
@RequestMapping("/api/v1/suspensions")
@Tag(name = "일시중단", description = "참가기관 일시중단 등록과 검토·확정")
public class SuspensionController {

  private final SuspensionService suspensionService;
  private final CurrentUserProvider currentUserProvider;

  public SuspensionController(
      SuspensionService suspensionService, CurrentUserProvider currentUserProvider) {
    this.suspensionService = suspensionService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "일시중단 목록 조회",
      description =
          "한 업무 영역의 일시중단. period 가 없으면 전부(최근 시작 순)다. 결제원 담당자는 전 기관을, 기관 담당자는 자기 기관만 본다. organizationId 는 결제원 담당자의 기관별 이력 보기다.")
  public PageResponse<SuspensionSummaryResponse> list(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @Parameter(description = "CURRENT(진행 중), UPCOMING(예정), PAST(지난). 없으면 전부")
          @RequestParam(required = false)
          SuspensionPeriod period,
      @Parameter(description = "이 기관만(결제원 담당자 전용)") @RequestParam(required = false)
          Long organizationId,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return suspensionService.list(
        currentUserProvider.require(),
        resolveBusiness(business),
        period,
        organizationId,
        PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/counts")
  @Operation(
      summary = "일시중단 시간 구분별 건수 조회",
      description = "목록의 필터 칩이 쓴다. pendingReview 는 아직 확정되지 않은 진행 중·예정 건이다.")
  public SuspensionCountsResponse counts(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @Parameter(description = "이 기관만(결제원 담당자 전용)") @RequestParam(required = false)
          Long organizationId) {
    return suspensionService.counts(
        currentUserProvider.require(), resolveBusiness(business), organizationId);
  }

  @GetMapping("/organizations")
  @Operation(
      summary = "일시중단 대상 기관 조회",
      description = "결제원 담당자는 그 업무에 참여하는 사용 중 참가기관 전부, 기관 담당자는 자기 기관 하나를 받는다. 등록 양식과 기관별 이력이 쓴다.")
  public List<SuspensionOrganizationResponse> organizations(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return suspensionService.organizations(
        currentUserProvider.require(), resolveBusiness(business));
  }

  @GetMapping("/other-organization-schedules")
  @Operation(
      summary = "같은 업무 다른 기관 일시중단 일정 조회",
      description =
          "결제원이 확정한 진행 중·예정 일시중단의 기관·일시·대상 업무만 시작 순으로 준다. 그 업무 담당자(결제원·참가기관)가 보고, 기관 담당자에게는 자기 기관 건을 뺀다.")
  public List<SuspensionScheduleResponse> otherOrganizationSchedules(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return suspensionService.otherOrganizationSchedules(
        currentUserProvider.require(), resolveBusiness(business));
  }

  @GetMapping("/registrable-organizations")
  @Operation(
      summary = "일시중단 등록 기관과 업무 조회",
      description =
          "등록 양식에서 고를 기관과 각 기관이 참여하는 업무. 등록은 기관 기준이라 업무 영역과 상관없다. 결제원 계정은 사용 중 참가기관 전부, 참가기관 계정은 자기 기관 하나를 받는다.")
  public List<SuspensionRegistrableOrganizationResponse> registrableOrganizations() {
    return suspensionService.registrableOrganizations(currentUserProvider.require());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "일시중단 등록",
      description =
          "multipart 로 양식(request, JSON)과 공문(files, 하나 이상)을 함께 받는다. 기관 일시중단은 공문이 없으면 400"
              + " SUSPENSION_DOCUMENT_REQUIRED(칸 files)다. 결제원 작업 안내는 공문 없이 올릴 수 있다. 검토 대기로 등록하고 그 업무 결제원"
              + " 담당자에게 메일·매터모스트 알림을 보낸다. 공문을 더 올리거나 바꾸는 것은 첨부 API 로 한다.")
  public SuspensionDetailResponse create(
      @Valid @RequestPart("request") SuspensionSaveRequest request,
      @Parameter(description = "공문 파일(여러 개 가능)")
          @RequestPart(value = "files", required = false)
          List<MultipartFile> files) {
    return suspensionService.create(
        currentUserProvider.require(), request, files == null ? List.of() : files);
  }

  @GetMapping("/{id}")
  @Operation(summary = "일시중단 상세 조회", description = "permissions 가 지금 할 수 있는 일이다.")
  public SuspensionDetailResponse get(@PathVariable Long id) {
    return suspensionService.get(currentUserProvider.require(), id);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "일시중단 수정",
      description =
          "등록한 기관 담당자나 그 업무 결제원 담당자가 고친다. 기관 담당자가 확인이 시작된 건을 고치면 검토 대기로 돌아간다. 결제원 담당자에게 매터모스트 알림이 간다. 완료한 건이면 409 SUSPENSION_COMPLETED.")
  public SuspensionDetailResponse update(
      @PathVariable Long id, @Valid @RequestBody SuspensionSaveRequest request) {
    return suspensionService.update(currentUserProvider.require(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "일시중단 삭제", description = "등록한 본인만 지운다. 결제원 담당자에게 매터모스트 알림이 간다.")
  public void delete(@PathVariable Long id) {
    suspensionService.delete(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/review")
  @Operation(summary = "일시중단 확인 시작", description = "그 업무 결제원 담당자가 확인을 시작한다. 검토 대기면 검토 중이 된다.")
  public SuspensionDetailResponse startReview(@PathVariable Long id) {
    return suspensionService.startReview(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/confirmation")
  @Operation(
      summary = "일시중단 확정",
      description = "그 업무 결제원 담당자가 조치 및 인수인계 사항을 쓰고 확정한다. 이미 확정한 건이면 사항만 고친다.")
  public SuspensionDetailResponse confirm(
      @PathVariable Long id, @Valid @RequestBody SuspensionHandoverRequest request) {
    return suspensionService.confirm(currentUserProvider.require(), id, request.handover());
  }

  static Business resolveBusiness(String slug) {
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
