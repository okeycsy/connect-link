package com.connectlink.auth.audit;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessLogRepository
    extends JpaRepository<AccessLog, Long>, JpaSpecificationExecutor<AccessLog> {

  @Modifying
  @Query("delete from AccessLog a where a.accessedAt < :before")
  int deleteOlderThan(@Param("before") LocalDateTime before);
}
