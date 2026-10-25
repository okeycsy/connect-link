package com.connectlink.suspension;

import com.connectlink.organization.Business;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** 일시중단 목록 조건. 기관 담당자는 {@code organizationCode} 로 자기 기관만 본다. */
final class SuspensionSpecifications {

  private SuspensionSpecifications() {}

  /**
   * @param organizationCode 이 기관과 결제원 작업 안내만(기관 담당자의 범위). 없으면 null
   * @param organizationId 기관별 이력에서 고른 기관. 없으면 null
   * @param period 시간 구분. 없으면 전부
   * @param includeUnlinked 관련 레일 업무가 없는 건도 이 업무 영역에 넣을지
   */
  static Specification<Suspension> of(
      Business business,
      String organizationCode,
      Long organizationId,
      SuspensionPeriod period,
      LocalDateTime now,
      boolean includeUnlinked) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      // 관련 레일 업무가 여럿이면 그중 하나가 이 업무면 든다.
      Subquery<Long> affected = query.subquery(Long.class);
      Root<SuspensionBusiness> sb = affected.from(SuspensionBusiness.class);
      affected
          .select(sb.get("id"))
          .where(cb.equal(sb.get("suspension"), root), cb.equal(sb.get("business"), business));
      if (includeUnlinked) {
        Subquery<Long> linked = query.subquery(Long.class);
        Root<SuspensionBusiness> any = linked.from(SuspensionBusiness.class);
        linked.select(any.get("id")).where(cb.equal(any.get("suspension"), root));
        predicates.add(cb.or(cb.exists(affected), cb.not(cb.exists(linked))));
      } else {
        predicates.add(cb.exists(affected));
      }
      if (organizationCode != null) {
        // 기관 담당자는 자기 기관 건과 결제원 작업 안내를 본다.
        predicates.add(
            cb.or(
                cb.equal(root.get("organization").get("code"), organizationCode),
                cb.equal(root.get("kind"), SuspensionKind.KFTC_WORK)));
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

  /** 지금(now) 진행 중이거나 until 전에 시작하는 건. 홈의 다가오는 일시중단이 쓴다. */
  static Specification<Suspension> activeOrStartingBefore(LocalDateTime now, LocalDateTime until) {
    return (root, query, cb) ->
        cb.and(cb.greaterThan(root.get("endAt"), now), cb.lessThan(root.get("startAt"), until));
  }

  /** 기관 이름, 중단 이유, 고른 대상 업무 이름에 검색어가 든 건. */
  static Specification<Suspension> matching(String keyword) {
    String pattern = "%" + keyword.trim().toLowerCase() + "%";
    return (root, query, cb) -> {
      Subquery<Long> work = query.subquery(Long.class);
      Root<SuspensionTargetWork> tw = work.from(SuspensionTargetWork.class);
      work.select(tw.get("id"))
          .where(cb.equal(tw.get("suspension"), root), cb.like(cb.lower(tw.get("name")), pattern));
      return cb.or(
          cb.like(cb.lower(root.get("organization").get("name")), pattern),
          cb.like(cb.lower(root.get("reason")), pattern),
          cb.exists(work));
    };
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
