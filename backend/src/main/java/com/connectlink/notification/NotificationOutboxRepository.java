package com.connectlink.notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

  Page<NotificationOutbox> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

  List<NotificationOutbox> findTop5ByChannelAndRecipientOrderByIdDesc(
      NotificationChannel channel, String recipient);
}
