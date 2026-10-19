package com.connectlink.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.BusinessAccessPolicy;
import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.search.dto.SearchHit;
import com.connectlink.search.dto.SearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchServiceTest {

  private static SearchProvider stub(SearchCategory category, List<SearchHit> hits) {
    return new SearchProvider() {
      @Override
      public SearchCategory category() {
        return category;
      }

      @Override
      public List<SearchHit> search(CurrentUser user, Business business, String query, int limit) {
        return hits;
      }
    };
  }

  @Test
  void 결과가_없는_묶음과_제공자가_없는_묶음은_빠지고_순서는_묶음_순서다() {
    SearchService service =
        new SearchService(
            List.of(
                stub(
                    SearchCategory.SUSPENSION,
                    List.of(new SearchHit("점검", null, "/cms/suspensions"))),
                stub(SearchCategory.CONTACT, List.of()),
                stub(SearchCategory.ORGANIZATION, List.of(new SearchHit("KB국민은행", null, "/x")))),
            new BusinessAccessPolicy());
    SearchResponse response = service.search(TestUsers.ADMIN, Business.CMS, " 가나 ");
    assertThat(response.query()).isEqualTo("가나");
    assertThat(response.groups())
        .extracting(g -> g.category())
        .containsExactly(SearchCategory.ORGANIZATION, SearchCategory.SUSPENSION);
  }

  @Test
  void 같은_묶음에_제공자가_둘이면_기동이_실패한다() {
    assertThatThrownBy(
            () ->
                new SearchService(
                    List.of(
                        stub(SearchCategory.CONTACT, List.of()),
                        stub(SearchCategory.CONTACT, List.of())),
                    new BusinessAccessPolicy()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CONTACT");
  }
}
