package com.connectlink.attachment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  List<Attachment> findByOwnerTypeAndOwnerIdOrderByCreatedAtAscIdAsc(
      AttachmentOwnerType ownerType, Long ownerId);

  List<Attachment> findByOwnerTypeAndOwnerIdInOrderByCreatedAtAscIdAsc(
      AttachmentOwnerType ownerType, Collection<Long> ownerIds);

  List<Attachment> findByStorageKeyStartingWith(String prefix);

  /** 반입 보안 검사 상태가 같은 파일(검사 중인 파일을 다시 확인할 때 쓴다). */
  List<Attachment> findByInspectionStatusOrderByIdAsc(AttachmentInspectionStatus status);

  /** 한 대상에서 내가 결재자인 파일(다른 사람이 올린 것). 팀장 본인이 올려 바로 승인된 파일은 빠진다. */
  @Query(
      "select a from Attachment a where a.ownerType = :ownerType and a.ownerId = :ownerId"
          + " and a.approverEmail = :email and a.createdBy <> :email"
          + " and a.approvalStatus <> com.connectlink.attachment.AttachmentApprovalStatus.NOT_REQUIRED"
          + " order by a.createdAt desc, a.id desc")
  List<Attachment> findForApprover(
      @Param("ownerType") AttachmentOwnerType ownerType,
      @Param("ownerId") Long ownerId,
      @Param("email") String email);

  /** 같은 대상에 같은 결재자로 이미 결재 대기 중인 다른 파일이 있는가(등록 한 건의 둘째 파일부터는 알림을 다시 보내지 않는다). */
  boolean existsByOwnerTypeAndOwnerIdAndApproverEmailAndApprovalStatusAndIdNot(
      AttachmentOwnerType ownerType,
      Long ownerId,
      String approverEmail,
      AttachmentApprovalStatus status,
      Long id);
}
