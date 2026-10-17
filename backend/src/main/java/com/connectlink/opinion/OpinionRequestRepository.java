package com.connectlink.opinion;

import com.connectlink.organization.Business;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpinionRequestRepository extends JpaRepository<OpinionRequest, Long> {

  /** 결제원 쪽 목록: 한 업무 영역의 요청 중 한 상태. */
  Page<OpinionRequest> findByBusinessAndStatus(
      Business business, OpinionRequestStatus status, Pageable pageable);

  long countByBusinessAndStatus(Business business, OpinionRequestStatus status);

  /** 기관 쪽 목록: 이 기관이 대상인 요청 중 한 상태. 작성중은 넘기지 않는다. */
  @Query(
      value =
          "select r from OpinionRequest r where r.business = :business and r.status = :status"
              + " and exists (select t.id from OpinionTarget t where t.request = r and"
              + " t.organization.code = :organizationCode)",
      countQuery =
          "select count(r) from OpinionRequest r where r.business = :business and r.status ="
              + " :status and exists (select t.id from OpinionTarget t where t.request = r and"
              + " t.organization.code = :organizationCode)")
  Page<OpinionRequest> findTargeted(
      @Param("business") Business business,
      @Param("status") OpinionRequestStatus status,
      @Param("organizationCode") String organizationCode,
      Pageable pageable);

  @Query(
      "select count(r) from OpinionRequest r where r.business = :business and r.status = :status"
          + " and exists (select t.id from OpinionTarget t where t.request = r and"
          + " t.organization.code = :organizationCode)")
  long countTargeted(
      @Param("business") Business business,
      @Param("status") OpinionRequestStatus status,
      @Param("organizationCode") String organizationCode);

  /** 전역 검색: 요청명에 검색어가 들어간 요청(상태 무관). 볼 수 있는지는 Service 가 거른다. */
  @Query(
      "select r from OpinionRequest r where r.business = :business and r.status in :statuses"
          + " and lower(r.title) like lower(concat('%', :query, '%')) order by r.dueDate desc,"
          + " r.id desc")
  List<OpinionRequest> searchByTitle(
      @Param("business") Business business,
      @Param("statuses") Collection<OpinionRequestStatus> statuses,
      @Param("query") String query,
      Pageable pageable);
}
