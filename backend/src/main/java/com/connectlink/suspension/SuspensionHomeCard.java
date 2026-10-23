package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.home.HomeCardContributor;
import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.HomeContribution;
import com.connectlink.home.Urgency;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardItem;
import com.connectlink.home.dto.HomeCard.HomeCardKpi;
import com.connectlink.home.dto.HomeCard.HomeCardMetric;
import com.connectlink.organization.Business;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 "이번 주 일시중단" 카드. 이번 주 월~금에 시작하는 일시중단을 시작일 칸에 놓는다. 결제원 담당자는 그 업무 전 기관을, 기관 담당자는 자기 기관 것만 본다. 오늘
 * 시작하는 건은 임박이다. 결제원 담당자에게는 아직 확정되지 않은 건 수를 함께 보인다. 그 밖의 사용자에게는 카드가 없다.
 */
@Component
@Transactional(readOnly = true)
public class SuspensionHomeCard implements HomeCardContributor {

  /** 한 주 칸에 놓는 건수 상한. 한 업무에 한 주 일시중단이 이보다 많을 일은 드물다. */
  private static final int MAX_ITEMS = 40;

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

  private final SuspensionRepository repository;
  private final SuspensionAccessPolicy accessPolicy;
  private final Clock clock;

  public SuspensionHomeCard(
      SuspensionRepository repository, SuspensionAccessPolicy accessPolicy, Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public HomeCardKey key() {
    return HomeCardKey.SUSPENSIONS;
  }

  @Override
  public HomeContribution contribute(CurrentUser user, Business business) {
    boolean manager = accessPolicy.canManage(user, business);
    String organizationCode;
    if (manager) {
      organizationCode = null;
    } else if (accessPolicy.isOrganizationMember(user, business)) {
      organizationCode = user.organizationCode();
    } else {
      return null;
    }
    LocalDate today = LocalDate.now(clock);
    LocalDate monday = today.with(DayOfWeek.MONDAY);
    Specification<Suspension> spec =
        SuspensionSpecifications.of(business, organizationCode, null, null, null)
            .and(
                SuspensionSpecifications.startingBetween(
                    monday.atStartOfDay(), monday.plusDays(5).atStartOfDay()));
    List<Suspension> week =
        repository
            .findAll(
                spec,
                PageRequest.of(
                    0, MAX_ITEMS, Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id"))))
            .getContent();

    List<HomeCardItem> items = new ArrayList<>();
    long todayCount = 0;
    long unconfirmed = 0;
    for (Suspension s : week) {
      LocalDate date = s.getStartAt().toLocalDate();
      boolean isToday = date.equals(today);
      if (isToday) {
        todayCount++;
      }
      if (s.getStatus() == SuspensionStatus.PENDING_REVIEW
          || s.getStatus() == SuspensionStatus.IN_REVIEW) {
        unconfirmed++;
      }
      items.add(
          new HomeCardItem(
              s.getOrganization().getName() + " " + s.getStartAt().format(TIME),
              s.getReason() + " · " + s.getStatus().getLabel(),
              date,
              isToday ? Urgency.SOON : Urgency.NONE,
              SuspensionNotifier.linkOf(s, business)));
    }
    String summary;
    if (week.isEmpty()) {
      summary = "이번 주 일시중단이 없습니다.";
    } else if (manager && unconfirmed > 0) {
      summary = "확정되지 않은 일시중단이 " + unconfirmed + "건 있습니다. 확인하고 조치 사항을 써 주세요.";
    } else if (todayCount > 0) {
      summary = "오늘 일시중단이 " + todayCount + "건 있습니다.";
    } else {
      summary = "이번 주 일시중단 " + week.size() + "건입니다.";
    }
    List<HomeCardMetric> metrics = new ArrayList<>();
    metrics.add(new HomeCardMetric("오늘", todayCount, "건"));
    metrics.add(new HomeCardMetric("이번 주", week.size(), "건"));
    if (manager) {
      metrics.add(new HomeCardMetric("확정 전", unconfirmed, "건"));
    }
    boolean urgent = todayCount > 0 || (manager && unconfirmed > 0);
    return new HomeContribution(
        new HomeCard(
            key(),
            key().getTitle(),
            HomeCardSource.REAL,
            urgent ? Urgency.SOON : Urgency.NONE,
            summary,
            metrics,
            null,
            items,
            "/" + business.getSlug() + "/" + key().getFeature()),
        new HomeCardKpi("이번 주 일시중단", week.size(), "건", urgent));
  }
}
