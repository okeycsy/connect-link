package com.connectlink.notification;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

  Page<NotificationOutbox> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

  List<NotificationOutbox> findTop5ByChannelAndRecipientOrderByIdDesc(
      NotificationChannel channel, String recipient);

  /** 지울 알림을 가리키던 발송 기록에서 알림 연결만 끊는다. 발송 기록은 남는다. */
  @Modifying(flushAutomatically = true)
  @Query(
      "update NotificationOutbox o set o.notificationId = null where o.notificationId in"
          + " :notificationIds")
  int detachNotifications(@Param("notificationIds") Collection<Long> notificationIds);
}
