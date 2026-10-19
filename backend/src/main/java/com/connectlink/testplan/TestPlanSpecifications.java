package com.connectlink.testplan;

import com.connectlink.organization.Business;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** 기관 테스트 목록 조건. 기관 담당자는 {@code organizationCode} 로 자기 기관이 대상인 테스트만 본다. */
final class TestPlanSpecifications {

  private TestPlanSpecifications() {}

  /**
   * @param organizationCode 이 기관이 대상인 테스트만(기관 담당자의 범위). 없으면 null
   * @param state 진행 구분. 없으면 전부
   */
  static Specification<TestPlan> of(
      Business business, String organizationCode, TestPlanState state) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("business"), business));
      if (organizationCode != null) {
        Subquery<Long> targeted = query.subquery(Long.class);
        Root<TestPlanTarget> target = targeted.from(TestPlanTarget.class);
        targeted
            .select(target.get("id"))
            .where(
                cb.equal(target.get("plan"), root),
                cb.equal(target.get("organization").get("code"), organizationCode));
        predicates.add(cb.exists(targeted));
      }
      if (state != null) {
        predicates.add(
            state == TestPlanState.COMPLETED
                ? cb.isNotNull(root.get("completedAt"))
                : cb.isNull(root.get("completedAt")));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  /** 개발 건 이름이나 단위업무에 검색어가 든 테스트. */
  static Specification<TestPlan> matching(String keyword) {
    String pattern = "%" + keyword.trim().toLowerCase() + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("title")), pattern),
            cb.like(cb.lower(root.get("subBusiness")), pattern));
  }

  /** 진행 중은 먼저 끝나는 순, 수행 완료는 최근 완료한 순, 전체는 최근 만든 순. */
  static Sort sortOf(TestPlanState state) {
    if (state == null) {
      return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }
    return switch (state) {
      case IN_PROGRESS -> Sort.by(Sort.Order.asc("endDate"), Sort.Order.desc("id"));
      case COMPLETED -> Sort.by(Sort.Order.desc("completedAt"), Sort.Order.desc("id"));
    };
  }
}
