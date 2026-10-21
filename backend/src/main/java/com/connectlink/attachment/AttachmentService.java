package com.connectlink.attachment;

import com.connectlink.attachment.dto.AttachmentResponse;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일. 기능 모듈은 자기 대상(ownerType, ownerId)으로 이 Service 를 쓰고, 누가 보고 올릴 수 있는지는 {@link
 * AttachmentOwnerAccess} 빈으로 알려 준다. 파일 내용은 {@link FileStorage} 로만 다룬다.
 */
@Service
@Transactional(readOnly = true)
public class AttachmentService {

  private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

  private final AttachmentRepository repository;
  private final AttachmentAccessPolicy accessPolicy;
  private final FileStorage fileStorage;
  private final AttachmentProperties properties;

  public AttachmentService(
      AttachmentRepository repository,
      AttachmentAccessPolicy accessPolicy,
      FileStorage fileStorage,
      AttachmentProperties properties) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.fileStorage = fileStorage;
    this.properties = properties;
  }

  public List<AttachmentResponse> list(
      CurrentUser user, AttachmentOwnerType ownerType, Long ownerId) {
    accessPolicy.ensureCanView(user, ownerType, ownerId);
    return repository.findByOwnerTypeAndOwnerIdOrderByCreatedAtAscIdAsc(ownerType, ownerId).stream()
        .map(AttachmentResponse::from)
        .toList();
  }

  /**
   * 여러 대상의 첨부를 한 번에 읽는다(대상 ID → 첨부, 첨부가 없는 대상은 빠진다). 메시지 목록처럼 대상이 많은 화면이 쓴다. 권한은 대상을 내려 주는 쪽이 이미
   * 확인했으므로 여기서 다시 보지 않는다.
   */
  public Map<Long, List<AttachmentResponse>> listOf(
      AttachmentOwnerType ownerType, Collection<Long> ownerIds) {
    if (ownerIds.isEmpty()) {
      return Map.of();
    }
    return repository
        .findByOwnerTypeAndOwnerIdInOrderByCreatedAtAscIdAsc(ownerType, ownerIds)
        .stream()
        .collect(
            Collectors.groupingBy(
                Attachment::getOwnerId,
                Collectors.mapping(AttachmentResponse::from, Collectors.toList())));
  }

  @Transactional
  public AttachmentResponse upload(
      CurrentUser user, AttachmentOwnerType ownerType, Long ownerId, MultipartFile file) {
    accessPolicy.ensureCanEdit(user, ownerType, ownerId);
    if (file == null || file.isEmpty()) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "비어 있는 파일은 올릴 수 없습니다.");
    }
    if (file.getSize() > properties.getMaxSize().toBytes()) {
      throw new BusinessException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          "FILE_TOO_LARGE",
          "파일은 " + properties.getMaxSize().toMegabytes() + "MB 까지 올릴 수 있습니다.");
    }
    String name = fileName(file);
    String key;
    try (InputStream in = file.getInputStream()) {
      key = fileStorage.store(in, name);
    } catch (IOException e) {
      throw new UncheckedIOException("파일을 저장하지 못했습니다: " + name, e);
    }
    Attachment attachment =
        new Attachment(ownerType, ownerId, name, contentType(file), file.getSize(), key);
    // 올린 사람·일시가 응답에 실리도록 바로 flush 한다.
    return AttachmentResponse.from(repository.saveAndFlush(attachment));
  }

  public AttachmentDownload download(CurrentUser user, Long id) {
    Attachment attachment = load(id);
    accessPolicy.ensureCanView(user, attachment.getOwnerType(), attachment.getOwnerId());
    Resource content = fileStorage.open(attachment.getStorageKey());
    if (!content.exists()) {
      throw BusinessException.notFound("ATTACHMENT_FILE_MISSING", "파일 내용이 저장소에 없습니다.");
    }
    return new AttachmentDownload(attachment, content);
  }

  @Transactional
  public void delete(CurrentUser user, Long id) {
    Attachment attachment = load(id);
    accessPolicy.ensureCanEdit(user, attachment.getOwnerType(), attachment.getOwnerId());
    repository.delete(attachment);
    try {
      fileStorage.delete(attachment.getStorageKey());
    } catch (IOException e) {
      // 메타데이터는 지웠으므로 내용은 남아도 접근할 길이 없다. 정리는 저장소 운영 몫이다.
      log.warn("첨부파일 내용을 지우지 못했습니다: {}", attachment.getStorageKey(), e);
    }
  }

  /**
   * 대상 하나의 첨부를 모두 지운다. 기능 모듈이 자기 대상(예: 작성중 의견 요청)을 지울 때 같은 트랜잭션에서 부른다. 권한은 대상을 지우는 쪽이 이미 확인했으므로 여기서
   * 다시 보지 않는다.
   */
  @Transactional
  public void deleteAllOf(AttachmentOwnerType ownerType, Long ownerId) {
    for (Attachment attachment :
        repository.findByOwnerTypeAndOwnerIdOrderByCreatedAtAscIdAsc(ownerType, ownerId)) {
      repository.delete(attachment);
      try {
        fileStorage.delete(attachment.getStorageKey());
      } catch (IOException e) {
        log.warn("첨부파일 내용을 지우지 못했습니다: {}", attachment.getStorageKey(), e);
      }
    }
  }

  private Attachment load(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("ATTACHMENT_NOT_FOUND", "첨부파일을 찾을 수 없습니다."));
  }

  /** 브라우저가 보낸 이름에서 경로 부분을 떼고 길이를 맞춘다. */
  private static String fileName(MultipartFile file) {
    String original = file.getOriginalFilename();
    if (original == null || original.isBlank()) {
      return "file";
    }
    String name = original.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).trim();
    if (name.isEmpty()) {
      return "file";
    }
    return name.length() > 255 ? name.substring(name.length() - 255) : name;
  }

  private static String contentType(MultipartFile file) {
    String type = file.getContentType();
    return type == null || type.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type;
  }
}
