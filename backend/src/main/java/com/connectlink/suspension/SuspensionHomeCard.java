package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.home.HomeCardContributor;
import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.HomeContribution;
import com.connectlink.home.Urgency;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardItem;
import com.connectlink.organization.Business;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 "다가오는 일시중단" 카드. 지금 진행 중인 건과 오늘부터 {@link #DAYS_AHEAD}일 안에 시작하는 건을 시작 순으로 보인다. 지난 날은 보이지 않고,
 * 일시중단이 몰리는 주말도 평일과 같이 넣는다(SHY 요청, 2026-10-24). 확인·확정하는 결제원 담당자에게만 그 업무 전 기관 것을 보이고, 기관 담당자 홈에는 두지
 * 않는다(SHY 요청, 2026-10-24). 진행 중이거나 오늘 시작하는 건은 임박이다.
 */
@Component
@Transactional(readOnly = true)
public class SuspensionHomeCard implements HomeCardContributor {

  /** 오늘부터 이 날수 안(오늘 포함)에 시작하는 건까지 보인다. 이번 주말과 다음 주말이 함께 든다. */
  static final int DAYS_AHEAD = 14;

  /** 카드에 놓는 건수 상한. 한 업무에 두 주 일시중단이 이보다 많을 일은 드물다. */
  private static final int MAX_ITEMS = 40;

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("M/d HH:mm");

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
    // 기관 담당자 홈에는 두지 않는다(자기 기관 일시중단은 일시중단 메뉴에서 본다). SHY 요청(2026-10-24).
    if (!accessPolicy.canManage(user, business)) {
      return null;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDate today = now.toLocalDate();
    Specification<Suspension> spec =
        SuspensionSpecifications.of(
                business, null, null, null, null, accessPolicy.handlesUnlinked(user))
            .and(
                SuspensionSpecifications.activeOrStartingBefore(
                    now, today.plusDays(DAYS_AHEAD + 1L).atStartOfDay()));
    List<Suspension> upcoming =
        repository
            .findAll(
                spec,
                PageRequest.of(
                    0, MAX_ITEMS, Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id"))))
            .getContent();

    List<HomeCardItem> items = new ArrayList<>();
    long ongoing = 0;
    long unconfirmed = 0;
    for (Suspension s : upcoming) {
      boolean running = !s.getStartAt().isAfter(now);
      if (running) {
        ongoing++;
      }
      if (s.getStatus() == SuspensionStatus.PENDING_REVIEW
          || s.getStatus() == SuspensionStatus.IN_REVIEW) {
        unconfirmed++;
      }
      LocalDate date = s.getStartAt().toLocalDate();
      items.add(
          new HomeCardItem(
              s.getOrganization().getName(),
              timeRange(s) + " · " + s.getStatus().getLabel(),
              date,
              running || date.equals(today) ? Urgency.SOON : Urgency.NONE,
              SuspensionNotifier.linkOf(s, business),
              s.getOrganization().getCode()));
    }
    String summary;
    if (unconfirmed > 0) {
      summary = "확정 전 " + unconfirmed + "건";
    } else if (ongoing > 0) {
      summary = "진행 중 " + ongoing + "건";
    } else {
      summary = "앞으로 " + DAYS_AHEAD + "일";
    }
    boolean urgent = ongoing > 0 || unconfirmed > 0;
    return HomeContribution.of(
        new HomeCard(
            key(),
            key().getTitle(),
            HomeCardSource.REAL,
            urgent ? Urgency.SOON : Urgency.NONE,
            summary,
            items,
            "/" + business.getSlug() + "/" + key().getFeature()));
  }

  /** "23:00 ~ 06:00"(끝이 다음 날이면 "23:00 ~ 10/26 06:00"). */
  private static String timeRange(Suspension s) {
    LocalDateTime start = s.getStartAt();
    LocalDateTime end = s.getEndAt();
    String endText =
        end.toLocalDate().equals(start.toLocalDate()) ? end.format(TIME) : end.format(DATE_TIME);
    return start.format(TIME) + " ~ " + endText;
  }
}
