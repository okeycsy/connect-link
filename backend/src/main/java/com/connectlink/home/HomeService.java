package com.connectlink.home;

import com.connectlink.auth.BusinessAccessPolicy;
import com.connectlink.auth.CurrentUser;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeResponse;
import com.connectlink.organization.Business;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 홈 화면 조립. 카드 자리({@link HomeCardKey}) 순서대로 기여자를 불러 모은다. 홈에는 이 사람에게 할 일이 있는 카드만 둔다: 기여자가 없는 자리, 기여자가
 * 이 사용자에게는 없는 자리라고 한 카드(null), 할 일 줄이 빈 카드는 뺀다. SHY 요청(2026-10-24).
 */
@Service
public class HomeService {

  private final Map<HomeCardKey, HomeCardContributor> contributors =
      new EnumMap<>(HomeCardKey.class);

  private final BusinessAccessPolicy businessAccessPolicy;

  public HomeService(
      List<HomeCardContributor> contributorBeans, BusinessAccessPolicy businessAccessPolicy) {
    this.businessAccessPolicy = businessAccessPolicy;
    for (HomeCardContributor contributor : contributorBeans) {
      HomeCardContributor previous = contributors.putIfAbsent(contributor.key(), contributor);
      if (previous != null) {
        throw new IllegalStateException(
            "홈 카드 자리 "
                + contributor.key()
                + " 에 기여자가 둘입니다: "
                + previous.getClass()
                + ", "
                + contributor.getClass());
      }
    }
  }

  /** 담당 업무가 아닌 업무 영역이면 403 이다(관리자는 전 업무). */
  public HomeResponse summary(CurrentUser user, Business business) {
    businessAccessPolicy.ensureCanView(user, business);
    List<HomeCard> cards = new ArrayList<>();
    for (HomeCardKey key : HomeCardKey.values()) {
      HomeCardContributor contributor = contributors.get(key);
      if (contributor == null) {
        continue;
      }
      HomeContribution contribution = contributor.contribute(user, business);
      if (contribution == null || contribution.card().items().isEmpty()) {
        continue;
      }
      cards.add(contribution.card());
    }
    return new HomeResponse(business, business.getLabel(), user.name(), cards);
  }
}
