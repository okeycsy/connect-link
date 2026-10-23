package com.connectlink.contact;

import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/** 담당자 목록 조건. 조회 범위({@link #scope})와 내부망 전용 검색·필터({@link #filter})를 따로 만들어 합친다. */
final class ContactSpecifications {

  /**
   * 결제원이 맨 위, 그다음 기관 이름순, 기관 안에서는 주담당 → 부담당, 이름순. 기관 구분은 내림차순이면 KFTC 가 FINANCE_ORGANIZATION 보다
   * 앞이다(담당자는 결제원·참가기관에만 있다).
   */
  static final Sort DEFAULT_SORT =
      Sort.by(
          Sort.Order.desc("organization.type"),
          Sort.Order.asc("organization.name"),
          Sort.Order.asc("rank"),
          Sort.Order.asc("name"),
          Sort.Order.asc("id"));

  private ContactSpecifications() {}

  /**
   * 사용자가 볼 수 있는 범위.
   *
   * @param ownOrganizationCode 기관 담당자의 자기 기관 코드. 이때는 결제원 담당자와 자기 기관 담당자만 본다. 결제원 담당자처럼 전 기관을 보면
   *     null
   */
  static Specification<Contact> scope(Business business, String ownOrganizationCode) {
    return (root, query, cb) -> {
      Predicate inBusiness = cb.equal(root.get("business"), business);
      if (ownOrganizationCode == null) {
        return inBusiness;
      }
      return cb.and(
          inBusiness,
          cb.or(
              cb.equal(root.get("organization").get("type"), OrganizationType.KFTC),
              cb.equal(root.get("organization").get("code"), ownOrganizationCode)));
    };
  }

  /**
   * 검색·필터. 값이 없는 조건은 빼고 모두 AND 다.
   *
   * @param keyword 이름, 기관 이름, 부서, 세부업무, 이메일에 든 글자
   * @param confirmBefore 최근 확인일이 이 시각 전인 담당자만(6개월 경과)
   * @param unreachableOnly 연락 불가로 표시된 담당자만
   */
  static Specification<Contact> filter(
      String keyword,
      Long organizationId,
      JobRole jobRole,
      LocalDateTime confirmBefore,
      boolean unreachableOnly) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (keyword != null && !keyword.isBlank()) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.<String>get("name")), pattern),
                cb.like(cb.lower(root.get("organization").<String>get("name")), pattern),
                cb.like(cb.lower(root.<String>get("department")), pattern),
                cb.like(cb.lower(cb.coalesce(root.<String>get("subBusiness"), "")), pattern),
                cb.like(root.<String>get("email"), pattern)));
      }
      if (organizationId != null) {
        predicates.add(cb.equal(root.get("organization").get("id"), organizationId));
      }
      if (jobRole != null) {
        predicates.add(cb.equal(root.get("jobRole"), jobRole));
      }
      if (confirmBefore != null) {
        predicates.add(cb.lessThan(root.get("lastConfirmedAt"), confirmBefore));
      }
      if (unreachableOnly) {
        predicates.add(cb.isNotNull(root.get("unreachableAt")));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
