package com.connectlink.testplan;

import com.connectlink.organization.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestPlanRepository
    extends JpaRepository<TestPlan, Long>, JpaSpecificationExecutor<TestPlan> {

  /** 결제원이 확인할 결과 수: 한 업무의 진행 중 테스트에서 기관이 오류로 등록했거나, 정상으로 등록했는데 결제원 의견이 없거나 지난 의견인 결과. 홈 카드가 쓴다. */
  @Query(
      "select count(r) from TestItemResult r where r.target.plan.business = :business"
          + " and r.target.plan.completedAt is null"
          + " and (r.result = com.connectlink.testplan.TestResult.FAILED"
          + " or (r.result = com.connectlink.testplan.TestResult.PASSED"
          + " and (r.feedbackAt is null or r.reportedAt > r.feedbackAt)))")
  long countAwaitingReview(@Param("business") Business business);
}
