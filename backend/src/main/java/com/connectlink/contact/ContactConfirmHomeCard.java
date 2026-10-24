package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.home.HomeCardContributor;
import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.HomeContribution;
import com.connectlink.home.Urgency;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardItem;
import com.connectlink.home.dto.HomeCard.HomeCardMetric;
import com.connectlink.organization.Business;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 "확인이 필요한 담당자" 카드. 최근 확인일에서 6개월이 지난 담당자를 오래된 순으로 보인다. 전 기관을 보는 결제원 담당자는 그 업무 전 기관, 기관 담당자는 자기 기관
 * 것이다(결제원 담당자 행은 기관 담당자가 고치지 못하므로 넣지 않는다). 그 밖의 사용자에게는 카드가 없다.
 */
@Component
@Transactional(readOnly = true)
public class ContactConfirmHomeCard implements HomeCardContributor {

  private static final int MAX_ITEMS = 5;

  private final ContactRepository repository;
  private final ContactAccessPolicy accessPolicy;
  private final Clock clock;

  public ContactConfirmHomeCard(
      ContactRepository repository, ContactAccessPolicy accessPolicy, Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public HomeCardKey key() {
    return HomeCardKey.CONTACT_CONFIRM;
  }

  @Override
  public HomeContribution contribute(CurrentUser user, Business business) {
    Specification<Contact> scope;
    if (accessPolicy.canViewAll(user, business)) {
      scope = ContactSpecifications.scope(business, null);
    } else if (accessPolicy.isOrganizationMember(user, business)) {
      String code = user.organizationCode();
      scope =
          ContactSpecifications.scope(business, code)
              .and((root, query, cb) -> cb.equal(root.get("organization").get("code"), code));
    } else {
      return null;
    }
    LocalDateTime threshold = LocalDateTime.now(clock).minus(ContactService.RECONFIRM_AFTER);
    Specification<Contact> overdueSpec =
        scope.and(ContactSpecifications.filter(null, null, null, threshold));
    long total = repository.count(scope);
    long overdue = repository.count(overdueSpec);
    String link = "/" + business.getSlug() + "/" + key().getFeature();
    List<HomeCardItem> items =
        repository
            .findAll(
                overdueSpec,
                PageRequest.of(
                    0, MAX_ITEMS, Sort.by(Sort.Order.asc("lastConfirmedAt"), Sort.Order.asc("id"))))
            .stream()
            .map(
                c ->
                    new HomeCardItem(
                        c.getOrganization().getName()
                            + " · "
                            + c.getName()
                            + " ("
                            + c.jobRoleLabel()
                            + ")",
                        "최근 확인 " + c.getLastConfirmedAt().toLocalDate(),
                        null,
                        Urgency.OVERDUE,
                        link + "?id=" + c.getId()))
            .toList();
    String summary =
        overdue == 0
            ? "최근 확인일이 6개월을 넘긴 담당자가 없습니다."
            : "최근 확인일이 6개월을 넘긴 담당자가 " + overdue + "명 있습니다. 고치거나 \"변동 없음\" 으로 확인해 주세요.";
    return HomeContribution.of(
        new HomeCard(
            key(),
            key().getTitle(),
            HomeCardSource.REAL,
            overdue > 0 ? Urgency.OVERDUE : Urgency.NONE,
            summary,
            List.of(
                new HomeCardMetric("6개월 초과", overdue, "명"),
                new HomeCardMetric("전체 담당자", total, "명")),
            null,
            items,
            link));
  }
}
