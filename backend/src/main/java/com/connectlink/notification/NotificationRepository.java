package com.connectlink.notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findByRecipientEmailOrderByCreatedAtDescIdDesc(
      String recipientEmail, Pageable pageable);

  Page<Notification> findByRecipientEmailAndReadAtIsNullOrderByCreatedAtDescIdDesc(
      String recipientEmail, Pageable pageable);

  List<Notification> findByRecipientEmailAndReadAtIsNull(String recipientEmail);

  long countByRecipientEmailAndReadAtIsNull(String recipientEmail);

  @Query("select n.id from Notification n where n.recipientEmail = :recipientEmail")
  List<Long> findIdsByRecipientEmail(@Param("recipientEmail") String recipientEmail);
}
