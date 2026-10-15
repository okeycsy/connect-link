package com.connectlink.organization;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.organization.dto.BusinessResponse;
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

  public OrganizationService(
      OrganizationRepository organizationRepository, OrganizationAccessPolicy accessPolicy) {
    this.organizationRepository = organizationRepository;
    this.accessPolicy = accessPolicy;
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

  public List<BusinessResponse> listBusinesses() {
    return Arrays.stream(Business.values()).map(BusinessResponse::from).toList();
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
    organization.replaceBusinesses(orEmpty(request.businesses()));
    // 감사 컬럼(등록자·등록 일시)이 응답에 실리도록 바로 flush 한다.
    return OrganizationDetailResponse.from(organizationRepository.saveAndFlush(organization));
  }

  @Transactional
  public OrganizationDetailResponse update(
      CurrentUser user, Long id, OrganizationUpdateRequest request) {
    accessPolicy.ensureCanManage(user);
    Organization organization = load(id);
    organization.update(
        request.name().trim(), normalizeDomain(request.emailDomain()), request.active());
    organization.replaceBusinesses(orEmpty(request.businesses()));
    // 수정자·수정 일시가 응답에 실리도록 바로 flush 한다.
    return OrganizationDetailResponse.from(organizationRepository.saveAndFlush(organization));
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
