package com.connectlink.organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrganizationRepository
    extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

  List<Organization> findAllByActiveTrue();

  Optional<Organization> findByCode(String code);

  boolean existsByCode(String code);
}
