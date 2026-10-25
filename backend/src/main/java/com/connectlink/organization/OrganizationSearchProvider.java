package com.connectlink.organization;

import com.connectlink.auth.CurrentUser;
import com.connectlink.search.SearchCategory;
import com.connectlink.search.SearchProvider;
import com.connectlink.search.dto.SearchHit;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 전역 검색의 "기관" 묶음. 레일의 업무에 참여하는 사용 중 기관을 이름·코드로 찾는다. 이름·코드는 공개 정보라 누구나 본다. */
@Component
@Transactional(readOnly = true)
public class OrganizationSearchProvider implements SearchProvider {

  private final OrganizationRepository organizationRepository;

  public OrganizationSearchProvider(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  @Override
  public SearchCategory category() {
    return SearchCategory.ORGANIZATION;
  }

  @Override
  public List<SearchHit> search(CurrentUser user, Business business, String query, int limit) {
    String needle = query.toLowerCase();
    return organizationRepository.findAllByActiveTrue().stream()
        .filter(o -> o.participatesIn(business))
        .filter(
            o ->
                o.getName().toLowerCase().contains(needle)
                    || o.getCode().toLowerCase().contains(needle))
        .sorted((a, b) -> a.getName().compareTo(b.getName()))
        .limit(limit)
        .map(
            o ->
                new SearchHit(
                    o.getName(),
                    o.getType().getLabel() + " · " + o.getCode(),
                    "/" + business.getSlug() + "/admin/organizations?org=" + o.getId()))
        .toList();
  }
}
