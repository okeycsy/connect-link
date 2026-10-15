package com.connectlink.attachment.dto;

import com.connectlink.attachment.Attachment;
import com.connectlink.attachment.AttachmentOwnerType;
import java.time.LocalDateTime;

/** 첨부파일 한 건(화면용). 내려받기 주소는 {@code /api/v1/attachments/{id}/download} 다. */
public record AttachmentResponse(
    Long id,
    AttachmentOwnerType ownerType,
    Long ownerId,
    String name,
    String contentType,
    long size,
    String uploadedBy,
    LocalDateTime uploadedAt) {

  public static AttachmentResponse from(Attachment attachment) {
    return new AttachmentResponse(
        attachment.getId(),
        attachment.getOwnerType(),
        attachment.getOwnerId(),
        attachment.getOriginalName(),
        attachment.getContentType(),
        attachment.getSizeBytes(),
        attachment.getCreatedBy(),
        attachment.getCreatedAt());
  }
}
