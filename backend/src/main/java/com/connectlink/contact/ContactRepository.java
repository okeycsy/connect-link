package com.connectlink.contact;

import com.connectlink.organization.Business;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContactRepository
    extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact> {

  @EntityGraph(attributePaths = "organization")
  List<Contact> findByEmailOrderByBusinessAscIdAsc(String email);

  @EntityGraph(attributePaths = "organization")
  List<Contact> findByEmailAndOrganizationCode(String email, String organizationCode);

  @EntityGraph(attributePaths = "organization")
  List<Contact> findByBusinessAndOrganizationIdOrderByRankAscNameAsc(
      Business business, Long organizationId);

  boolean existsByOrganizationIdAndBusinessAndEmail(
      Long organizationId, Business business, String email);

  long countByBusinessAndOrganizationId(Business business, Long organizationId);

  /** 최근 확인일이 이 시각 전인 담당자. 6개월 미확인 알림 작업이 쓴다. */
  @EntityGraph(attributePaths = "organization")
  List<Contact> findByLastConfirmedAtBefore(LocalDateTime threshold);
}
