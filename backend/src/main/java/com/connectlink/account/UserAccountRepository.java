package com.connectlink.account;

import com.connectlink.organization.Business;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  @EntityGraph(attributePaths = "organization")
  Optional<UserAccount> findByEmail(String email);

  @EntityGraph(attributePaths = "organization")
  List<UserAccount> findByStatus(AccountStatus status);

  boolean existsByEmailAndStatus(String email, AccountStatus status);

  long countByAdminTrueAndStatus(AccountStatus status);

  /** 반려가 아닌 계정이 이 이메일로 있는지. 반려된 이메일은 다시 신청할 수 있다. */
  boolean existsByEmailAndStatusNot(String email, AccountStatus status);

  /**
   * 가입 승인 화면의 신청 목록: 한 업무를 신청에 넣은 계정 중 한 상태. {@code reviewedOnly} 면 승인·반려를 거친 계정만 센다(시드로 만든 결제원
   * 계정처럼 가입 절차를 거치지 않은 사용 중 계정은 빠진다).
   */
  @EntityGraph(attributePaths = "organization")
  @Query(
      value =
          "select a from UserAccount a where a.status = :status"
              + " and (:reviewedOnly = false or a.reviewedAt is not null)"
              + " and exists (select b.id from UserAccountBusiness b where b.account = a and"
              + " b.business = :business)",
      countQuery =
          "select count(a) from UserAccount a where a.status = :status"
              + " and (:reviewedOnly = false or a.reviewedAt is not null)"
              + " and exists (select b.id from UserAccountBusiness b where b.account = a and"
              + " b.business = :business)")
  Page<UserAccount> findApplications(
      @Param("business") Business business,
      @Param("status") AccountStatus status,
      @Param("reviewedOnly") boolean reviewedOnly,
      Pageable pageable);

  @Query(
      "select count(a) from UserAccount a where a.status = :status"
          + " and (:reviewedOnly = false or a.reviewedAt is not null)"
          + " and exists (select b.id from UserAccountBusiness b where b.account = a and"
          + " b.business = :business)")
  long countApplications(
      @Param("business") Business business,
      @Param("status") AccountStatus status,
      @Param("reviewedOnly") boolean reviewedOnly);
}
