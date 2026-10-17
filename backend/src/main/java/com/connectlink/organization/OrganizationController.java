package com.connectlink.organization;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.organization.dto.BusinessResponse;
import com.connectlink.organization.dto.FinanceSectorResponse;
import com.connectlink.organization.dto.JobRoleResponse;
import com.connectlink.organization.dto.OrganizationCreateRequest;
import com.connectlink.organization.dto.OrganizationDetailResponse;
import com.connectlink.organization.dto.OrganizationResponse;
import com.connectlink.organization.dto.OrganizationSearchCondition;
import com.connectlink.organization.dto.OrganizationUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기관·업무 기준정보. 이름·구분만 담는 목록과 업무 영역 목록은 가입 화면(로그인 전)도 쓰므로 공개 API다. 참여 업무·이메일 도메인까지 다루는 관리 API 는 관리자만
 * 쓴다.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "기준정보", description = "기관과 업무 영역")
public class OrganizationController {

  private final OrganizationService organizationService;
  private final CurrentUserProvider currentUserProvider;

  public OrganizationController(
      OrganizationService organizationService, CurrentUserProvider currentUserProvider) {
    this.organizationService = organizationService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping("/organizations")
  @Operation(summary = "기관 목록 조회", description = "사용 중인 기관을 구분·이름순으로 돌려준다. 로그인 없이 쓸 수 있다.")
  public List<OrganizationResponse> listOrganizations() {
    return organizationService.listActive();
  }

  @GetMapping("/job-roles")
  @Operation(
      summary = "직무 목록 조회",
      description = "가입 화면의 직무 선택지. 전산이면 세부 직무를 하나 이상 고른다. 로그인 없이 쓸 수 있다.")
  public List<JobRoleResponse> listJobRoles() {
    return organizationService.listJobRoles();
  }

  @GetMapping("/finance-sectors")
  @Operation(summary = "업권 목록 조회", description = "참가기관의 업권(은행, 저축은행, 증권 …) 고정 목록이다.")
  public List<FinanceSectorResponse> listFinanceSectors() {
    return Arrays.stream(FinanceSector.values()).map(FinanceSectorResponse::from).toList();
  }

  @GetMapping("/businesses")
  @Operation(summary = "업무 영역 목록 조회", description = "왼쪽 레일과 가입 화면의 업무 선택지에 쓰는 고정 목록이다.")
  public List<BusinessResponse> listBusinesses() {
    return organizationService.listBusinesses();
  }

  @GetMapping("/organizations/search")
  @Operation(
      summary = "기관 기준정보 검색 (관리자)",
      description = "이름·코드 검색어, 구분, 참여 업무(slug), 사용 여부로 거른다. 구분 순(결제원 → 참가기관 → 이용기관) 뒤 이름순.")
  public PageResponse<OrganizationDetailResponse> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) OrganizationType type,
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam(required = false)
          String business,
      @RequestParam(required = false) Boolean active,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    OrganizationSearchCondition condition =
        new OrganizationSearchCondition(keyword, type, resolveBusiness(business), active);
    return organizationService.search(
        currentUserProvider.require(), condition, PageRequest.of(page, Math.min(size, 100)));
  }

  @GetMapping("/organizations/{id}")
  @Operation(summary = "기관 기준정보 상세 조회 (관리자)")
  public OrganizationDetailResponse get(@PathVariable Long id) {
    return organizationService.get(currentUserProvider.require(), id);
  }

  @PostMapping("/organizations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "기관 등록 (관리자)", description = "같은 코드가 이미 있으면 409.")
  public OrganizationDetailResponse create(@Valid @RequestBody OrganizationCreateRequest request) {
    return organizationService.create(currentUserProvider.require(), request);
  }

  @PutMapping("/organizations/{id}")
  @Operation(summary = "기관 수정 (관리자)", description = "이름, 이메일 도메인, 사용 여부, 참여 업무를 통째로 바꾼다.")
  public OrganizationDetailResponse update(
      @PathVariable Long id, @Valid @RequestBody OrganizationUpdateRequest request) {
    return organizationService.update(currentUserProvider.require(), id, request);
  }

  private static Business resolveBusiness(String slug) {
    if (slug == null || slug.isBlank()) {
      return null;
    }
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
