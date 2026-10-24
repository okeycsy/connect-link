package com.connectlink.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.BusinessAccessPolicy;
import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeResponse;
import com.connectlink.organization.Business;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeServiceTest {

  private static final HomeCard.HomeCardItem TODO =
      new HomeCard.HomeCardItem("할 일", null, null, Urgency.NONE, null);

  private static HomeCardContributor stub(HomeCardKey key) {
    return stub(key, List.of(TODO));
  }

  private static HomeCardContributor stub(HomeCardKey key, List<HomeCard.HomeCardItem> items) {
    return new HomeCardContributor() {
      @Override
      public HomeCardKey key() {
        return key;
      }

      @Override
      public HomeContribution contribute(CurrentUser user, Business business) {
        return HomeContribution.of(
            new HomeCard(
                key, key.getTitle(), HomeCardSource.REAL, Urgency.NONE, null, items, null));
      }
    };
  }

  @Test
  void 기여자가_없는_자리는_홈에_없다() {
    HomeService service =
        new HomeService(List.of(stub(HomeCardKey.OPINION_REPLIES)), new BusinessAccessPolicy());

    HomeResponse home = service.summary(TestUsers.ADMIN, Business.LOAN_TRANSFER);

    assertThat(home.cards()).extracting(HomeCard::key).containsExactly(HomeCardKey.OPINION_REPLIES);
  }

  @Test
  void 할_일_줄이_빈_카드는_홈에_없다() {
    HomeService service =
        new HomeService(
            List.of(
                stub(HomeCardKey.UPDATE_CAMPAIGN, List.of()),
                stub(HomeCardKey.OPINION_REPLIES),
                stub(HomeCardKey.SUSPENSIONS)),
            new BusinessAccessPolicy());

    HomeResponse home = service.summary(TestUsers.ADMIN, Business.ACCOUNT_INFO);

    assertThat(home.cards())
        .extracting(HomeCard::key)
        .containsExactly(HomeCardKey.OPINION_REPLIES, HomeCardKey.SUSPENSIONS);
  }

  @Test
  void 기여자가_이_사용자에게는_없는_자리라고_하면_카드를_뺀다() {
    HomeCardContributor hidden =
        new HomeCardContributor() {
          @Override
          public HomeCardKey key() {
            return HomeCardKey.SIGNUP_APPROVAL;
          }

          @Override
          public HomeContribution contribute(CurrentUser user, Business business) {
            return null;
          }
        };
    HomeService service =
        new HomeService(
            List.of(stub(HomeCardKey.OPINION_REPLIES), hidden), new BusinessAccessPolicy());

    HomeResponse home = service.summary(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO);

    assertThat(home.cards()).extracting(HomeCard::key).containsExactly(HomeCardKey.OPINION_REPLIES);
  }

  @Test
  void 같은_자리에_기여자가_둘이면_기동하지_않는다() {
    assertThatThrownBy(
            () ->
                new HomeService(
                    List.of(stub(HomeCardKey.SUSPENSIONS), stub(HomeCardKey.SUSPENSIONS)),
                    new BusinessAccessPolicy()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SUSPENSIONS");
  }
}
