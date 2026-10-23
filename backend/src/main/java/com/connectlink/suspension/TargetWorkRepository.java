package com.connectlink.suspension;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TargetWorkRepository extends JpaRepository<TargetWork, Long> {

  List<TargetWork> findAllByOrderBySortOrderAscNameAsc();

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Long id);
}
