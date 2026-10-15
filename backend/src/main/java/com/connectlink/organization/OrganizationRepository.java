package com.connectlink.organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  List<Organization> findAllByActiveTrue();

  Optional<Organization> findByCode(String code);
}
