package com.connectlink.testplan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestPlanTargetContactRepository
    extends JpaRepository<TestPlanTargetContact, Long> {

  List<TestPlanTargetContact> findByEmail(String email);
}
