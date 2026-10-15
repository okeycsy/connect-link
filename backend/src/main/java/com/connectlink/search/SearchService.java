package com.connectlink.search;

import com.connectlink.auth.BusinessAccessPolicy;
import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.search.dto.SearchGroup;
import com.connectlink.search.dto.SearchHit;
import com.connectlink.search.dto.SearchResponse;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 전역 검색 조립. 묶음({@link SearchCategory}) 순서대로 제공자를 부르고, 결과가 없거나 제공자가 없는 묶음은 뺀다. */
@Service
public class SearchService {

  /** 묶음마다 보여 줄 최대 건수. 팔레트는 훑어보는 곳이라 길게 주지 않는다. */
  static final int LIMIT_PER_GROUP = 5;

  private final Map<SearchCategory, SearchProvider> providers = new EnumMap<>(SearchCategory.class);

  private final BusinessAccessPolicy businessAccessPolicy;

  public SearchService(
      List<SearchProvider> providerBeans, BusinessAccessPolicy businessAccessPolicy) {
    this.businessAccessPolicy = businessAccessPolicy;
    for (SearchProvider provider : providerBeans) {
      SearchProvider previous = providers.putIfAbsent(provider.category(), provider);
      if (previous != null) {
        throw new IllegalStateException(
            "검색 묶음 "
                + provider.category()
                + " 에 제공자가 둘입니다: "
                + previous.getClass()
                + ", "
                + provider.getClass());
      }
    }
  }

  /** 담당 업무가 아닌 업무 영역이면 403 이다(관리자는 전 업무). */
  public SearchResponse search(CurrentUser user, Business business, String query) {
    businessAccessPolicy.ensureCanView(user, business);
    String trimmed = query == null ? "" : query.trim();
    List<SearchGroup> groups = new ArrayList<>();
    if (trimmed.isEmpty()) {
      return new SearchResponse(trimmed, business, groups);
    }
    for (SearchCategory category : SearchCategory.values()) {
      SearchProvider provider = providers.get(category);
      if (provider == null) {
        continue;
      }
      List<SearchHit> hits = provider.search(user, business, trimmed, LIMIT_PER_GROUP);
      if (!hits.isEmpty()) {
        groups.add(new SearchGroup(category, category.getLabel(), provider.source(), hits));
      }
    }
    return new SearchResponse(trimmed, business, groups);
  }
}
