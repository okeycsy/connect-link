package com.connectlink.organization;

import com.connectlink.auth.BusinessAccessPolicy;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.organization.dto.OrganizationOverviewItem;
import com.connectlink.organization.dto.OrganizationOverviewResponse;
import com.connectlink.organization.dto.OrganizationOverviewSection;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 한눈에 보기 조립. 결제원 담당자(관리자 포함)가 맡은 업무에 참여하는 참가기관 하나를 고르면 칸({@link OrganizationOverviewSectionKey})
 * 순서대로 기여자를 부른다. 기여자가 null 을 준 칸(그 기능에서 볼 수 없다)과 기여자가 없는 칸은 뺀다.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationOverviewService {

  private final Map<OrganizationOverviewSectionKey, OrganizationOverviewContributor> contributors =
      new EnumMap<>(OrganizationOverviewSectionKey.class);

  private final OrganizationService organizationService;
  private final BusinessAccessPolicy businessAccessPolicy;

  public OrganizationOverviewService(
      List<OrganizationOverviewContributor> contributorBeans,
      OrganizationService organizationService,
      BusinessAccessPolicy businessAccessPolicy) {
    this.organizationService = organizationService;
    this.businessAccessPolicy = businessAccessPolicy;
    for (OrganizationOverviewContributor contributor : contributorBeans) {
      OrganizationOverviewContributor previous =
          contributors.putIfAbsent(contributor.key(), contributor);
      if (previous != null) {
        throw new IllegalStateException(
            "기관 한눈에 보기 칸 "
                + contributor.key()
                + " 에 기여자가 둘입니다: "
                + previous.getClass()
                + ", "
                + contributor.getClass());
      }
    }
  }

  /**
   * 결제원 담당자만 본다(기관 담당자는 자기 기관 화면이 따로 있다). 담당 업무가 아니면 403, 그 업무에 참여하지 않거나 사용하지 않는 기관·참가기관이 아닌 기관이면
   * 404 다.
   */
  public OrganizationOverviewResponse overview(
      CurrentUser user, Business business, Long organizationId) {
    if (!user.admin() && user.organizationType() != OrganizationType.KFTC) {
      throw new AccessDeniedException("기관 한눈에 보기는 결제원 담당자만 봅니다.");
    }
    businessAccessPolicy.ensureCanView(user, business);
    Organization organization = organizationService.getActive(organizationId);
    if (organization.getType() != OrganizationType.FINANCE_ORGANIZATION
        || !organization.participatesIn(business)) {
      throw BusinessException.notFound(
          "ORGANIZATION_NOT_FOUND", business.getLabel() + " 에 참여하는 참가기관이 아닙니다.");
    }
    List<OrganizationOverviewSection> sections = new ArrayList<>();
    for (OrganizationOverviewSectionKey key : OrganizationOverviewSectionKey.values()) {
      OrganizationOverviewContributor contributor = contributors.get(key);
      if (contributor == null) {
        continue;
      }
      List<OrganizationOverviewItem> items = contributor.contribute(user, business, organization);
      if (items != null) {
        sections.add(
            new OrganizationOverviewSection(
                key, key.getTitle(), items, "/" + business.getSlug() + "/" + key.getFeature()));
      }
    }
    return new OrganizationOverviewResponse(
        organization.getId(),
        organization.getCode(),
        organization.getName(),
        organization.getSector() == null ? null : organization.getSector().getLabel(),
        business,
        sections);
  }
}
