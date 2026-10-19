package com.connectlink.testplan;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestPlanRecipientRepository extends JpaRepository<TestPlanRecipient, Long> {

  List<TestPlanRecipient> findByAccountId(Long accountId);
}
