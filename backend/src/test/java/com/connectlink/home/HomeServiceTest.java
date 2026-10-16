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

  private static HomeCardContributor stub(HomeCardKey key) {
    return new HomeCardContributor() {
      @Override
      public HomeCardKey key() {
        return key;
      }

      @Override
      public HomeContribution contribute(CurrentUser user, Business business) {
        return new HomeContribution(
            new HomeCard(
                key,
                key.getTitle(),
                HomeCardSource.REAL,
                Urgency.NONE,
                null,
                List.of(),
                null,
                List.of(),
                null),
            new HomeCard.HomeCardKpi(key.getTitle(), 1, "건", false));
      }
    };
  }

  @Test
  void 기여자가_없는_자리는_연결_예정_카드로_채운다() {
    HomeService service =
        new HomeService(List.of(stub(HomeCardKey.OPINION_REPLIES)), new BusinessAccessPolicy());

    HomeResponse home = service.summary(TestUsers.ADMIN, Business.LOAN_TRANSFER);

    assertThat(home.cards()).hasSize(HomeCardKey.values().length);
    assertThat(home.cards().get(0).source()).isEqualTo(HomeCardSource.UNAVAILABLE);
    assertThat(home.cards().get(0).link()).isEqualTo("/loan-transfer/contacts");
    assertThat(home.cards().get(2).source()).isEqualTo(HomeCardSource.REAL);
    assertThat(home.kpis()).hasSize(1);
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

    assertThat(home.cards()).hasSize(HomeCardKey.values().length - 1);
    assertThat(home.cards()).noneMatch(c -> c.key() == HomeCardKey.SIGNUP_APPROVAL);
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
