package com.connectlink.testplan;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationOverviewContributor;
import com.connectlink.organization.OrganizationOverviewSectionKey;
import com.connectlink.organization.dto.OrganizationOverviewItem;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 한눈에 보기의 "진행 중 기관 테스트" 칸. 그 기관이 대상인 진행 중 테스트와 그 기관의 수행률·미등록 요구사항 수. 수행 기간이 지났는데 기관이 다 끝내지 못했으면
 * 눈에 띄게 보인다. 그 업무 결제원 담당자만 본다.
 */
@Component
@Transactional(readOnly = true)
public class TestPlanOrganizationOverview implements OrganizationOverviewContributor {

  private final TestPlanRepository repository;
  private final TestPlanAccessPolicy accessPolicy;
  private final Clock clock;

  public TestPlanOrganizationOverview(
      TestPlanRepository repository, TestPlanAccessPolicy accessPolicy, Clock clock) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Override
  public OrganizationOverviewSectionKey key() {
    return OrganizationOverviewSectionKey.TEST_PLAN;
  }

  @Override
  public List<OrganizationOverviewItem> contribute(
      CurrentUser user, Business business, Organization organization) {
    if (!accessPolicy.canManage(user, business)) {
      return null;
    }
    LocalDate today = LocalDate.now(clock);
    List<OrganizationOverviewItem> items = new ArrayList<>();
    for (TestPlan plan :
        repository.findAll(
            TestPlanSpecifications.of(business, organization.getCode(), TestPlanState.IN_PROGRESS),
            TestPlanSpecifications.sortOf(TestPlanState.IN_PROGRESS))) {
      Optional<TestPlanTarget> target = plan.findTargetByCode(organization.getCode());
      int itemCount = plan.getItems().size();
      boolean done =
          target.map(t -> itemCount > 0 && t.getCompletedCount() == itemCount).orElse(false);
      int unreported =
          target
              .map(t -> TestPlanService.toTargetSummary(t, plan.getItems()).unreported())
              .orElse(itemCount);
      String subtitle =
          plan.getStartDate()
              + " ~ "
              + plan.getEndDate()
              + " · "
              + plan.getSubBusiness()
              + (unreported > 0 ? " · 미등록 " + unreported + "개" : "");
      items.add(
          OrganizationOverviewItem.of(
              plan.getTitle(),
              subtitle,
              done ? "수행 완료" : "수행률 " + target.map(TestPlanTarget::getProgressRate).orElse(0) + "%",
              !done && plan.getEndDate().isBefore(today),
              TestPlanNotifier.linkOf(plan)));
    }
    return items;
  }
}
