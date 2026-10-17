package com.connectlink.organization;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.history.ChangeHistoryCommand;
import com.connectlink.history.ChangeHistoryService;
import com.connectlink.history.FieldChanges;
import com.connectlink.history.HistoryTargetType;
import com.connectlink.organization.dto.BusinessResponse;
import com.connectlink.organization.dto.JobRoleResponse;
import com.connectlink.organization.dto.OrganizationCreateRequest;
import com.connectlink.organization.dto.OrganizationDetailResponse;
import com.connectlink.organization.dto.OrganizationResponse;
import com.connectlink.organization.dto.OrganizationSearchCondition;
import com.connectlink.organization.dto.OrganizationUpdateRequest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관·업무 기준정보. 다른 모듈은 Repository 대신 이 Service를 쓴다. 관리 기능의 권한은 {@link OrganizationAccessPolicy} 가
 * 정한다.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationService {

  private final OrganizationRepository organizationRepository;
  private final OrganizationAccessPolicy accessPolicy;
  private final ChangeHistoryService changeHistoryService;

  public OrganizationService(
      OrganizationRepository organizationRepository,
      OrganizationAccessPolicy accessPolicy,
      ChangeHistoryService changeHistoryService) {
    this.organizationRepository = organizationRepository;
    this.accessPolicy = accessPolicy;
    this.changeHistoryService = changeHistoryService;
  }

  public List<OrganizationResponse> listActive() {
    // 구분은 enum 순서(결제원 → 참가기관 → 이용기관), 같은 구분 안에서는 이름순.
    return organizationRepository.findAllByActiveTrue().stream()
        .sorted(
            Comparator.comparing((Organization o) -> o.getType().ordinal())
                .thenComparing(Organization::getName))
        .map(OrganizationResponse::from)
        .toList();
  }

  public Organization getByCode(String code) {
    return organizationRepository
        .findByCode(code)
        .orElseThrow(
            () -> BusinessException.notFound("ORGANIZATION_NOT_FOUND", "기관을 찾을 수 없습니다: " + code));
  }

  /** 사용 중인 기관 하나. 없거나 사용하지 않는 기관이면 404 다. 가입 신청처럼 기관을 고르는 곳이 쓴다. */
  public Organization getActive(Long id) {
    return organizationRepository
        .findById(id)
        .filter(Organization::isActive)
        .orElseThrow(() -> BusinessException.notFound("ORGANIZATION_NOT_FOUND", "기관을 찾을 수 없습니다."));
  }

  /** 이메일 도메인이 같은 사용 중인 기관들. 가입 화면이 회사 이메일로 기관을 미리 골라 줄 때 쓴다. */
  public List<Organization> findActiveByEmailDomain(String emailDomain) {
    if (emailDomain == null || emailDomain.isBlank()) {
      return List.of();
    }
    return organizationRepository.findAllByActiveTrueAndEmailDomainIgnoreCase(emailDomain);
  }

  public List<BusinessResponse> listBusinesses() {
    return Arrays.stream(Business.values()).map(BusinessResponse::from).toList();
  }

  public List<JobRoleResponse> listJobRoles() {
    return Arrays.stream(JobRole.values()).map(JobRoleResponse::from).toList();
  }

  /** 관리 화면의 기관 목록. 검색어(이름·코드), 구분, 참여 업무, 사용 여부로 거른다. */
  public PageResponse<OrganizationDetailResponse> search(
      CurrentUser user, OrganizationSearchCondition condition, Pageable pageable) {
    accessPolicy.ensureCanManage(user);
    return PageResponse.from(
        organizationRepository.findAll(OrganizationSpecifications.search(condition), pageable),
        OrganizationDetailResponse::from);
  }

  public OrganizationDetailResponse get(CurrentUser user, Long id) {
    accessPolicy.ensureCanManage(user);
    return OrganizationDetailResponse.from(load(id));
  }

  @Transactional
  public OrganizationDetailResponse create(CurrentUser user, OrganizationCreateRequest request) {
    accessPolicy.ensureCanManage(user);
    if (organizationRepository.existsByCode(request.code())) {
      throw BusinessException.conflict(
          "ORGANIZATION_CODE_DUPLICATE", "이미 있는 기관 코드입니다: " + request.code());
    }
    Organization organization =
        new Organization(
            request.code(),
            request.name().trim(),
            request.type(),
            normalizeDomain(request.emailDomain()));
    organization.changeSector(request.sector());
    organization.replaceBusinesses(orEmpty(request.businesses()));
    // 감사 컬럼(등록자·등록 일시)이 응답에 실리도록 바로 flush 한다.
    Organization saved = organizationRepository.saveAndFlush(organization);
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.created(
            HistoryTargetType.ORGANIZATION,
            saved.getId(),
            null,
            saved.getName() + " 등록",
            FieldChanges.compare()
                .value("code", "기관 코드", null, saved.getCode())
                .value("name", "기관명", null, saved.getName())
                .value("type", "기관 구분", null, saved.getType())
                .value("emailDomain", "이메일 도메인", null, saved.getEmailDomain())
                .value("sector", "업권", null, saved.getSector())
                .value("businesses", "참여 업무", null, FieldChanges.join(saved.getBusinesses()))
                .toList()));
    return OrganizationDetailResponse.from(saved);
  }

  @Transactional
  public OrganizationDetailResponse update(
      CurrentUser user, Long id, OrganizationUpdateRequest request) {
    accessPolicy.ensureCanManage(user);
    Organization organization = load(id);
    FieldChanges changes =
        FieldChanges.compare()
            .field("name", "기관명", organization.getName(), request.name().trim())
            .field(
                "emailDomain",
                "이메일 도메인",
                organization.getEmailDomain(),
                normalizeDomain(request.emailDomain()))
            .field("active", "사용 여부", organization.isActive(), request.active())
            .field(
                "sector",
                "업권",
                organization.getSector(),
                organization.getType() == OrganizationType.FINANCE_ORGANIZATION
                    ? request.sector()
                    : null)
            .field(
                "businesses",
                "참여 업무",
                FieldChanges.join(organization.getBusinesses()),
                FieldChanges.join(orEmpty(request.businesses())));
    organization.update(
        request.name().trim(),
        normalizeDomain(request.emailDomain()),
        request.active(),
        request.sector());
    organization.replaceBusinesses(orEmpty(request.businesses()));
    // 수정자·수정 일시가 응답에 실리도록 바로 flush 한다.
    Organization saved = organizationRepository.saveAndFlush(organization);
    // 바뀐 항목이 없으면 이력을 남기지 않는다.
    changeHistoryService.record(
        user,
        ChangeHistoryCommand.updated(
            HistoryTargetType.ORGANIZATION,
            saved.getId(),
            null,
            saved.getName() + " 수정",
            changes.toList()));
    return OrganizationDetailResponse.from(saved);
  }

  private Organization load(Long id) {
    return organizationRepository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("ORGANIZATION_NOT_FOUND", "기관을 찾을 수 없습니다."));
  }

  private static String normalizeDomain(String emailDomain) {
    if (emailDomain == null || emailDomain.isBlank()) {
      return null;
    }
    return emailDomain.trim().toLowerCase();
  }

  private static Set<Business> orEmpty(Set<Business> businesses) {
    return businesses == null ? Set.of() : businesses;
  }
}
