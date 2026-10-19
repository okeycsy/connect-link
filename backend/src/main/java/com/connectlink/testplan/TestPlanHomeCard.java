package com.connectlink.testplan;

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
import com.connectlink.home.dto.HomeCard.HomeCardProgress;
import com.connectlink.organization.Business;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 "진행 중 기관 테스트" 카드. 결제원 담당자는 그 업무의 진행 중 테스트와 확인할 결과(오류, 또는 정상인데 결제원 의견이 없는 결과)를, 기관 담당자는 자기 기관이
 * 대상인 진행 중 테스트와 자기 기관 수행률·미등록 요구사항을 본다. 기간이 지난 테스트는 초과, 사흘 안에 끝나면 임박이다. 그 밖의 사용자에게는 카드가 없다.
 */
@Component
@Transactional(readOnly = true)
public class TestPlanHomeCard implements HomeCardContributor {

  /** 카드에 늘어놓는 테스트 수 상한. */
  private static final int MAX_ITEMS = 5;

  /** 이 날수 안에 끝나는 테스트는 임박이다. */
  private static final int SOON_DAYS = 3;

  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;
  private final Clock clock;

  public TestPlanHomeCard(
      TestPlanRepository repository, TestPlanAccessPolicy accessPolicy, Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public HomeCardKey key() {
    return HomeCardKey.TEST_PROGRESS;
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
    var spec = TestPlanSpecifications.of(business, organizationCode, TestPlanState.IN_PROGRESS);
    // 진행 중 테스트 수는 한 업무에 많지 않아 모두 읽어 집계한다(요구사항·대상은 묶음 조회).
    List<TestPlan> running =
        repository.findAll(spec, TestPlanSpecifications.sortOf(TestPlanState.IN_PROGRESS));

    long overdue = running.stream().filter(p -> p.isOverdue(today)).count();
    List<HomeCardItem> items = new ArrayList<>();
    long unreported = 0;
    long failed = 0;
    long doneTargets = 0;
    long totalTargets = 0;
    for (TestPlan plan : running) {
      int itemCount = plan.getItems().size();
      String subtitle;
      if (manager) {
        long completed =
            plan.getTargets().stream()
                .filter(t -> itemCount > 0 && t.getCompletedCount() == itemCount)
                .count();
        doneTargets += completed;
        totalTargets += plan.getTargets().size();
        subtitle =
            plan.getSubBusiness()
                + " · 수행률 "
                + plan.getProgressRate()
                + "% · 완료 기관 "
                + completed
                + "/"
                + plan.getTargets().size();
      } else {
        Optional<TestPlanTarget> own = plan.findTargetByCode(organizationCode);
        int rate = own.map(TestPlanTarget::getProgressRate).orElse(0);
        var summary = own.map(t -> TestPlanService.toTargetSummary(t, plan.getItems()));
        unreported += summary.map(s -> s.unreported()).orElse(itemCount);
        failed += summary.map(s -> s.failed()).orElse(0);
        subtitle = plan.getSubBusiness() + " · 우리 기관 수행률 " + rate + "%";
      }
      if (items.size() < MAX_ITEMS) {
        items.add(
            new HomeCardItem(
                plan.getTitle(),
                subtitle,
                plan.getEndDate(),
                urgencyOf(plan, today),
                TestPlanNotifier.linkOf(plan)));
      }
    }

    long awaiting = manager ? repository.countAwaitingReview(business) : 0;
    List<HomeCardMetric> metrics = new ArrayList<>();
    metrics.add(new HomeCardMetric("진행 중", running.size(), "건"));
    if (manager) {
      metrics.add(new HomeCardMetric("확인할 결과", awaiting, "건"));
    } else {
      metrics.add(new HomeCardMetric("미등록 요구사항", unreported, "건"));
      metrics.add(new HomeCardMetric("오류", failed, "건"));
    }
    if (overdue > 0) {
      metrics.add(new HomeCardMetric("기간 지남", overdue, "건"));
    }

    String summary;
    if (running.isEmpty()) {
      summary = "진행 중인 기관 테스트가 없습니다.";
    } else if (manager && awaiting > 0) {
      summary = "확인할 기관 결과가 " + awaiting + "건 있습니다. 미니 수행률과 의견을 남겨 주세요.";
    } else if (!manager && unreported > 0) {
      summary = "아직 결과를 등록하지 않은 요구사항이 " + unreported + "건 있습니다.";
    } else if (overdue > 0) {
      summary = "수행 기간이 지난 테스트가 " + overdue + "건 있습니다.";
    } else {
      summary = "진행 중인 기관 테스트 " + running.size() + "건입니다.";
    }
    boolean urgent = overdue > 0 || (manager ? awaiting > 0 : unreported > 0);
    Urgency urgency = overdue > 0 ? Urgency.OVERDUE : urgent ? Urgency.SOON : Urgency.NONE;
    return new HomeContribution(
        new HomeCard(
            key(),
            key().getTitle(),
            HomeCardSource.REAL,
            urgency,
            summary,
            metrics,
            manager && totalTargets > 0
                ? new HomeCardProgress(doneTargets, totalTargets, "완료 기관")
                : null,
            items,
            "/" + business.getSlug() + "/" + key().getFeature()),
        new HomeCardKpi("진행 중 기관 테스트", running.size(), "건", urgent));
  }

  private static Urgency urgencyOf(TestPlan plan, LocalDate today) {
    if (plan.isOverdue(today)) {
      return Urgency.OVERDUE;
    }
    return plan.getEndDate().isBefore(today.plusDays(SOON_DAYS + 1L)) ? Urgency.SOON : Urgency.NONE;
  }
}
