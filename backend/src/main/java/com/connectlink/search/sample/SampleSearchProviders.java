package com.connectlink.search.sample;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.search.SearchCategory;
import com.connectlink.search.SearchProvider;
import com.connectlink.search.SearchSource;
import com.connectlink.search.dto.SearchHit;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 기능 프로젝트가 붙기 전까지 전역 검색 묶음을 채우는 샘플 제공자들. 홈 카드의 샘플 기여자와 같은 방식이다(SHY 결정 2026-10-15, "샘플로 채우기").
 *
 * <p>전부 가상 데이터이며 local·demo·test 프로필에만 있다. 각 기능 프로젝트가 실제 {@link SearchProvider} 를 만들면 그 묶음의 샘플 클래스를
 * 지운다.
 */
public final class SampleSearchProviders {

  private SampleSearchProviders() {}

  /** 제목·부제에 검색어가 들어간 항목만, 레일의 업무 slug 로 링크를 만들어 돌려준다. */
  abstract static class SampleProvider implements SearchProvider {

    private final SearchCategory category;
    private final String feature;
    private final List<String[]> rows;

    SampleProvider(SearchCategory category, String feature, List<String[]> rows) {
      this.category = category;
      this.feature = feature;
      this.rows = rows;
    }

    @Override
    public SearchCategory category() {
      return category;
    }

    @Override
    public SearchSource source() {
      return SearchSource.SAMPLE;
    }

    @Override
    public List<SearchHit> search(CurrentUser user, Business business, String query, int limit) {
      String needle = query.toLowerCase();
      return rows.stream()
          .filter(r -> r[0].toLowerCase().contains(needle) || r[1].toLowerCase().contains(needle))
          .limit(limit)
          .map(r -> new SearchHit(r[0], r[1], "/" + business.getSlug() + "/" + feature))
          .toList();
    }
  }

  @Component
  @Profile({"local", "demo", "test"})
  static class Contacts extends SampleProvider {
    Contacts() {
      super(
          SearchCategory.CONTACT,
          "contacts",
          List.of(
              new String[] {"김민준", "KB국민은행 · 현업 · 주담당"},
              new String[] {"이준호", "신한은행 · 전산(대외계) · 주담당"},
              new String[] {"박소영", "SBI저축은행 · 현업 · 부담당"},
              new String[] {"최유진", "삼성증권 · 전산(개발) · 주담당"},
              new String[] {"정재원", "타파보험 · 현업 · 주담당"},
              new String[] {"윤하은", "자차카드 · 전산(운영) · 부담당"}));
    }
  }
}
