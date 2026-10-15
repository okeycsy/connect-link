package com.connectlink.notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findByRecipientEmailOrderByCreatedAtDescIdDesc(
      String recipientEmail, Pageable pageable);

  Page<Notification> findByRecipientEmailAndReadAtIsNullOrderByCreatedAtDescIdDesc(
      String recipientEmail, Pageable pageable);

  List<Notification> findByRecipientEmailAndReadAtIsNull(String recipientEmail);

  long countByRecipientEmailAndReadAtIsNull(String recipientEmail);
}
