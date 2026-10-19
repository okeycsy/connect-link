package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.NetworkZoneResolver;
import com.connectlink.organization.Business;
import com.connectlink.search.SearchCategory;
import com.connectlink.search.SearchProvider;
import com.connectlink.search.dto.SearchHit;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전역 검색의 "담당자" 묶음. 레일의 업무에서 이름, 기관, 부서, 세부업무, 이메일로 찾는다. 담당자 검색은 내부망 전용이라(명세) 인터넷망에서는 결과가 없다. 범위는
 * 담당자 목록과 같다.
 */
@Component
@Transactional(readOnly = true)
public class ContactSearchProvider implements SearchProvider {

  private final ContactRepository repository;
  private final ContactAccessPolicy accessPolicy;
  private final NetworkZoneResolver networkZoneResolver;

  public ContactSearchProvider(
      ContactRepository repository,
      ContactAccessPolicy accessPolicy,
      NetworkZoneResolver networkZoneResolver) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.networkZoneResolver = networkZoneResolver;
  }

  @Override
  public SearchCategory category() {
    return SearchCategory.CONTACT;
  }

  @Override
  public List<SearchHit> search(CurrentUser user, Business business, String query, int limit) {
    if (!networkZoneResolver.isInternal()) {
      return List.of();
    }
    String ownOrganizationCode;
    if (accessPolicy.canViewAll(user, business)) {
      ownOrganizationCode = null;
    } else if (accessPolicy.isOrganizationMember(user, business)) {
      ownOrganizationCode = user.organizationCode();
    } else {
      return List.of();
    }
    return repository
        .findAll(
            ContactSpecifications.scope(business, ownOrganizationCode)
                .and(ContactSpecifications.filter(query, null, null, null)),
            PageRequest.of(0, limit, ContactSpecifications.DEFAULT_SORT))
        .stream()
        .map(
            c ->
                new SearchHit(
                    c.getName() + " · " + c.getOrganization().getName(),
                    c.jobRoleLabel()
                        + " · "
                        + c.getRank().getLabel()
                        + " · 내선 "
                        + c.getExtensionPhone(),
                    "/" + business.getSlug() + "/contacts?id=" + c.getId()))
        .toList();
  }
}
