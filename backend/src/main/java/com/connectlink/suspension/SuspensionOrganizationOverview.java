package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationOverviewContributor;
import com.connectlink.organization.OrganizationOverviewSectionKey;
import com.connectlink.organization.dto.OrganizationOverviewItem;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 한눈에 보기의 "일시중단" 칸. 그 기관의 그 업무 일시중단 가운데 진행 중·예정인 것과 끝난 지 {@value #RECENT_DAYS}일이 안 된 것. 진행 중이 맨
 * 위에서 눈에 띄게, 그다음 예정(가까운 순), 지난 것(최근 순)이다. 그 업무 결제원 담당자만 본다.
 */
@Component
@Transactional(readOnly = true)
public class SuspensionOrganizationOverview implements OrganizationOverviewContributor {

  static final int RECENT_DAYS = 30;

  private final SuspensionRepository repository;
  private final SuspensionAccessPolicy accessPolicy;
  private final Clock clock;

  public SuspensionOrganizationOverview(
      SuspensionRepository repository, SuspensionAccessPolicy accessPolicy, Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public OrganizationOverviewSectionKey key() {
    return OrganizationOverviewSectionKey.SUSPENSION;
  }

  @Override
  public List<OrganizationOverviewItem> contribute(
      CurrentUser user, Business business, Organization organization) {
    if (!accessPolicy.canManage(user, business)) {
      return null;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime recent = now.minusDays(RECENT_DAYS);
    return repository
        .findAll(
            SuspensionSpecifications.of(
                business,
                null,
                organization.getId(),
                null,
                now,
                accessPolicy.handlesUnlinked(user)),
            Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id")))
        .stream()
        .filter(s -> s.getEndAt().isAfter(recent))
        .sorted(
            Comparator.comparing((Suspension s) -> rank(periodOf(s, now)))
                .thenComparing(
                    (a, b) ->
                        periodOf(a, now) == SuspensionPeriod.PAST
                            ? b.getStartAt().compareTo(a.getStartAt())
                            : a.getStartAt().compareTo(b.getStartAt())))
        .map(
            s -> {
              SuspensionPeriod period = periodOf(s, now);
              return OrganizationOverviewItem.of(
                  s.targetWorkNames(),
                  SuspensionNotifier.periodText(s.getStartAt(), s.getEndAt())
                      + " · "
                      + s.getStatus().getLabel(),
                  period.getLabel(),
                  period == SuspensionPeriod.CURRENT,
                  SuspensionNotifier.linkOf(s, business));
            })
        .toList();
  }

  private static SuspensionPeriod periodOf(Suspension s, LocalDateTime now) {
    if (s.getStartAt().isAfter(now)) {
      return SuspensionPeriod.UPCOMING;
    }
    return s.getEndAt().isAfter(now) ? SuspensionPeriod.CURRENT : SuspensionPeriod.PAST;
  }

  private static int rank(SuspensionPeriod period) {
    return switch (period) {
      case CURRENT -> 0;
      case UPCOMING -> 1;
      case PAST -> 2;
    };
  }
}
