package com.connectlink.suspension;

import com.connectlink.organization.Business;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** 일시중단 목록 조건. 기관 담당자는 {@code organizationCode} 로 자기 기관만 본다. */
final class SuspensionSpecifications {

  private SuspensionSpecifications() {}

  /**
   * @param organizationCode 이 기관만(기관 담당자의 범위). 없으면 null
   * @param organizationId 기관별 이력에서 고른 기관. 없으면 null
   * @param period 시간 구분. 없으면 전부
   */
  static Specification<Suspension> of(
      Business business,
      String organizationCode,
      Long organizationId,
      SuspensionPeriod period,
      LocalDateTime now) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("business"), business));
      if (organizationCode != null) {
        predicates.add(cb.equal(root.get("organization").get("code"), organizationCode));
      }
      if (organizationId != null) {
        predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
      }
      if (period != null) {
        switch (period) {
          case CURRENT -> {
            predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), now));
            predicates.add(cb.greaterThan(root.get("endAt"), now));
          }
          case UPCOMING -> predicates.add(cb.greaterThan(root.get("startAt"), now));
          case PAST -> predicates.add(cb.lessThanOrEqualTo(root.get("endAt"), now));
        }
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  /** 시작 시각이 이 구간 [from, to) 에 드는 건. 홈의 이번 주 칸이 쓴다. */
  static Specification<Suspension> startingBetween(LocalDateTime from, LocalDateTime to) {
    return (root, query, cb) ->
        cb.and(
            cb.greaterThanOrEqualTo(root.get("startAt"), from),
            cb.lessThan(root.get("startAt"), to));
  }

  /** 기관 이름이나 중단 이유에 검색어가 든 건. */
  static Specification<Suspension> matching(String keyword) {
    String pattern = "%" + keyword.trim().toLowerCase() + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("organization").get("name")), pattern),
            cb.like(cb.lower(root.get("reason")), pattern));
  }

  /** 진행 중은 먼저 끝나는 순, 예정은 먼저 시작하는 순, 지난 건과 전체는 최근 시작한 순. */
  static Sort sortOf(SuspensionPeriod period) {
    if (period == null) {
      return Sort.by(Sort.Order.desc("startAt"), Sort.Order.desc("id"));
    }
    return switch (period) {
      case CURRENT -> Sort.by(Sort.Order.asc("endAt"), Sort.Order.asc("id"));
      case UPCOMING -> Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id"));
      case PAST -> Sort.by(Sort.Order.desc("startAt"), Sort.Order.desc("id"));
    };
  }
}
