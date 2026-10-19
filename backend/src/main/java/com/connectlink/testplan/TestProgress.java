package com.connectlink.testplan;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 수행률과 수행 완료 계산. 요구사항(기관별) 수행률은 {@link TestItemResult#effectiveRate()} 이고, 아직 결과가 없으면 0 이다. 기관 수행률은
 * 그 기관 요구사항 수행률의 평균, 테스트 수행률은 대상 기관 × 요구사항 전체의 평균이다(반올림). 모든 대상 기관의 모든 요구사항이 완료면 테스트 수행 완료다.
 */
final class TestProgress {

  private TestProgress() {}

  /** 테스트 하나의 기관별·전체 수행률과 완료 여부를 다시 계산해 엔티티에 반영한다. */
  static void recalculate(TestPlan plan, LocalDateTime now) {
    List<TestItem> items = plan.getItems();
    List<TestPlanTarget> targets = plan.getTargets();
    long sum = 0;
    int cells = 0;
    boolean allCompleted = !items.isEmpty() && !targets.isEmpty();
    for (TestPlanTarget target : targets) {
      long targetSum = 0;
      int completed = 0;
      for (TestItem item : items) {
        TestItemResult result = target.findResult(item.getId()).orElse(null);
        int rate = result == null ? 0 : result.effectiveRate();
        targetSum += rate;
        if (result != null && result.isCompleted()) {
          completed++;
        }
      }
      target.updateProgress(average(targetSum, items.size()), completed);
      if (completed < items.size()) {
        allCompleted = false;
      }
      sum += targetSum;
      cells += items.size();
    }
    plan.updateProgress(average(sum, cells), allCompleted, now);
  }

  /** 대상 기관 하나의 결과별 건수. 결과가 없는 요구사항(미등록)은 세지 않는다. */
  static Map<TestResult, Integer> countByResult(TestPlanTarget target, List<TestItem> items) {
    Map<TestResult, Integer> counts = new EnumMap<>(TestResult.class);
    for (TestResult r : TestResult.values()) {
      counts.put(r, 0);
    }
    for (TestItem item : items) {
      target.findResult(item.getId()).ifPresent(r -> counts.merge(r.getResult(), 1, Integer::sum));
    }
    return counts;
  }

  private static int average(long sum, int count) {
    return count == 0 ? 0 : (int) Math.round((double) sum / count);
  }
}
