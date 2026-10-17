package com.connectlink.opinion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpinionReminderRepository extends JpaRepository<OpinionReminder, Long> {

  List<OpinionReminder> findByRequest_IdOrderByCreatedAtDescIdDesc(Long requestId);
}
