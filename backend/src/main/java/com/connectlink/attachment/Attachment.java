package com.connectlink.attachment;

import com.connectlink.common.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 첨부파일 한 건. 파일 내용은 {@link FileStorage} 에 있고 여기에는 그 키와 원래 이름·종류·크기만 둔다. 올린 사람·일시는 감사 컬럼이다. */
@Entity
@Table(name = "attachment")
public class Attachment extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "owner_type", nullable = false, length = 30)
  private AttachmentOwnerType ownerType;

  @Column(name = "owner_id", nullable = false)
  private Long ownerId;

  /** 올릴 때의 파일 이름. 내려받을 때 이 이름을 돌려준다. */
  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  /** 저장소 안의 위치. 저장소 구현이 정한다. */
  @Column(name = "storage_key", nullable = false, length = 200)
  private String storageKey;

  protected Attachment() {}

  public Attachment(
      AttachmentOwnerType ownerType,
      Long ownerId,
      String originalName,
      String contentType,
      long sizeBytes,
      String storageKey) {
    this.ownerType = ownerType;
    this.ownerId = ownerId;
    this.originalName = originalName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.storageKey = storageKey;
  }

  public Long getId() {
    return id;
  }

  public AttachmentOwnerType getOwnerType() {
    return ownerType;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getStorageKey() {
    return storageKey;
  }
}
