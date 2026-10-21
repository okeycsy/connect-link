package com.connectlink.attachment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByOwnerTypeAndOwnerIdOrderByCreatedAtAscIdAsc(
      AttachmentOwnerType ownerType, Long ownerId);

  List<Attachment> findByOwnerTypeAndOwnerIdInOrderByCreatedAtAscIdAsc(
      AttachmentOwnerType ownerType, Collection<Long> ownerIds);
}
