package com.connectlink.attachment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByOwnerTypeAndOwnerIdOrderByCreatedAtAscIdAsc(
      AttachmentOwnerType ownerType, Long ownerId);

  List<Attachment> findByOwnerTypeAndOwnerIdInOrderByCreatedAtAscIdAsc(
      AttachmentOwnerType ownerType, Collection<Long> ownerIds);

  List<Attachment> findByStorageKeyStartingWith(String prefix);

  /** 내가 결재할 파일(다른 사람이 올린 것). 팀장 본인이 올려 바로 승인된 파일은 빠진다. */
  @Query(
      "select a from Attachment a where a.approverEmail = :email and a.createdBy <> :email"
          + " and a.approvalStatus in :statuses order by a.createdAt desc, a.id desc")
  Page<Attachment> findToApprove(
      @Param("email") String email,
      @Param("statuses") Collection<AttachmentApprovalStatus> statuses,
      Pageable pageable);

  /** 내가 올려 결재를 받는(받은) 파일. */
  @Query(
      "select a from Attachment a where a.createdBy = :email"
          + " and a.approvalStatus in :statuses order by a.createdAt desc, a.id desc")
  Page<Attachment> findRequestedBy(
      @Param("email") String email,
      @Param("statuses") Collection<AttachmentApprovalStatus> statuses,
      Pageable pageable);

  long countByApproverEmailAndApprovalStatus(String approverEmail, AttachmentApprovalStatus status);
}
