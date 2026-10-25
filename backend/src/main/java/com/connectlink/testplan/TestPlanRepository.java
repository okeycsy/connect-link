package com.connectlink.testplan;

import com.connectlink.organization.Business;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestPlanRepository
    extends JpaRepository<TestPlan, Long>, JpaSpecificationExecutor<TestPlan> {

  /** 자동 리마인드 대상: 수행 종료일이 그날이고 아직 수행 완료가 아닌 테스트(업무 상관없이). */
  List<TestPlan> findByEndDateAndCompletedAtIsNull(LocalDate endDate);

  /** 자동 리마인드 대상: 테스트데이터를 요청했고 회신 기한이 그날인 테스트. */
  List<TestPlan> findByTestDataRequestedTrueAndTestDataDueDate(LocalDate dueDate);

  /** 결제원이 확인할 결과 수: 한 업무의 진행 중 테스트에서 기관이 오류로 등록했거나, 정상으로 등록했는데 결제원 의견이 없거나 지난 의견인 결과. 홈 카드가 쓴다. */
  @Query(
      "select count(r) from TestItemResult r where r.target.plan.business = :business"
          + " and r.target.plan.completedAt is null"
          + " and (r.result = com.connectlink.testplan.TestResult.FAILED"
          + " or (r.result = com.connectlink.testplan.TestResult.PASSED"
          + " and (r.feedbackAt is null or r.reportedAt > r.feedbackAt)))")
  long countAwaitingReview(@Param("business") Business business);
}
