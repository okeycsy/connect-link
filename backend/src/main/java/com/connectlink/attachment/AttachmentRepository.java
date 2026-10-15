package com.connectlink.attachment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByOwnerTypeAndOwnerIdOrderByCreatedAtAscIdAsc(
      AttachmentOwnerType ownerType, Long ownerId);
}
