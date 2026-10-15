package com.connectlink.organization;

import com.connectlink.common.BusinessException;
import com.connectlink.organization.dto.BusinessResponse;
import com.connectlink.organization.dto.OrganizationResponse;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기관·업무 기준정보 조회. 다른 모듈은 Repository 대신 이 Service를 쓴다. */
@Service
@Transactional(readOnly = true)
public class OrganizationService {

  private final OrganizationRepository organizationRepository;

  public OrganizationService(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
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
}
