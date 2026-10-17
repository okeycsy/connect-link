package com.connectlink.suspension;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuspensionRepository
    extends JpaRepository<Suspension, Long>, JpaSpecificationExecutor<Suspension> {

  /** 종료 시각이 지난 확정 건. 완료로 바꿀 대상이다. */
  List<Suspension> findByStatusAndEndAtLessThanEqual(SuspensionStatus status, LocalDateTime now);

  /** 전 영업일 취합: 이 구간에 시작하는 한 상태의 건(전 업무). 업무, 시작 시각, 기관 이름순. */
  @Query(
      "select s from Suspension s join fetch s.organization o where s.status = :status and"
          + " s.startAt >= :from and s.startAt < :to order by s.business, s.startAt, o.name")
  List<Suspension> findStartingBetween(
      @Param("status") SuspensionStatus status,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to);
}
