package com.connectlink.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long> {

  Page<ChangeHistory> findByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
      HistoryTargetType targetType, Long targetId, Pageable pageable);
}
