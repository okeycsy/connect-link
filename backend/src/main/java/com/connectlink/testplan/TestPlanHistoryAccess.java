package com.connectlink.testplan;

import com.connectlink.auth.CurrentUser;
import com.connectlink.history.HistoryTargetAccess;
import com.connectlink.history.HistoryTargetType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기관 테스트 모듈의 변경 이력 권한. 테스트 이력(내용·요구사항·대상 기관·알림 대상)은 그 업무의 결제원 담당자가 본다(대상 기관 목록이 들어 있어서). 기관별 결과 이력은
 * 거기에 더해 그 기관 담당자가 본다. 다른 기관의 결과 이력은 볼 수 없다.
 */
final class TestPlanHistoryAccess {

  private TestPlanHistoryAccess() {}

  @Component
  @Transactional(readOnly = true)
  static class PlanHistory implements HistoryTargetAccess {

    private final TestPlanRepository repository;
    private final TestPlanAccessPolicy accessPolicy;

    PlanHistory(TestPlanRepository repository, TestPlanAccessPolicy accessPolicy) {
      this.repository = repository;
      this.accessPolicy = accessPolicy;
    }

    @Override
    public HistoryTargetType targetType() {
      return HistoryTargetType.TEST_PLAN;
    }

    @Override
    public boolean canView(CurrentUser user, Long targetId) {
      return repository
          .findById(targetId)
          .map(p -> accessPolicy.canManage(user, p.getBusiness()))
          .orElse(false);
    }
  }

  @Component
  @Transactional(readOnly = true)
  static class TargetHistory implements HistoryTargetAccess {

    private final TestPlanTargetRepository repository;
    private final TestPlanAccessPolicy accessPolicy;

    TargetHistory(TestPlanTargetRepository repository, TestPlanAccessPolicy accessPolicy) {
      this.repository = repository;
      this.accessPolicy = accessPolicy;
    }

    @Override
    public HistoryTargetType targetType() {
      return HistoryTargetType.TEST_PLAN_TARGET;
    }

    @Override
    public boolean canView(CurrentUser user, Long targetId) {
      return repository
          .findById(targetId)
          .map(t -> accessPolicy.canViewTarget(user, t))
          .orElse(false);
    }
  }
}
