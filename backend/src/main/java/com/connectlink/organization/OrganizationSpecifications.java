package com.connectlink.organization;

import com.connectlink.organization.dto.OrganizationSearchCondition;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/** 기관 목록 검색 조건을 JPA Specification 으로 만든다. 정렬은 구분(결제원 → 참가기관 → 이용기관) 뒤 이름순. */
final class OrganizationSpecifications {

  private OrganizationSpecifications() {}

  static Specification<Organization> search(OrganizationSearchCondition condition) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (condition.keyword() != null && !condition.keyword().isBlank()) {
        String pattern = "%" + condition.keyword().trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)));
      }
      if (condition.type() != null) {
        predicates.add(cb.equal(root.get("type"), condition.type()));
      }
      if (condition.active() != null) {
        predicates.add(cb.equal(root.get("active"), condition.active()));
      }
      if (condition.business() != null) {
        // distinct 없이 걸러내려고 join 대신 exists 서브쿼리를 쓴다.
        Subquery<Long> participation = query.subquery(Long.class);
        Root<OrganizationBusiness> ob = participation.from(OrganizationBusiness.class);
        participation
            .select(ob.get("id"))
            .where(
                cb.equal(ob.get("organization"), root),
                cb.equal(ob.get("business"), condition.business()));
        predicates.add(cb.exists(participation));
      }
      if (!Long.class.equals(query.getResultType())) {
        query.orderBy(cb.asc(typeOrder(root, cb)), cb.asc(root.get("name")));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static Expression<Integer> typeOrder(Root<Organization> root, CriteriaBuilder cb) {
    CriteriaBuilder.Case<Integer> order = cb.selectCase();
    for (OrganizationType type : OrganizationType.values()) {
      order = order.when(cb.equal(root.get("type"), type), type.ordinal());
    }
    return order.otherwise(OrganizationType.values().length);
  }
}
