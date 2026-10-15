package com.connectlink.attachment;

import com.connectlink.attachment.dto.AttachmentResponse;
import com.connectlink.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/attachments")
@Tag(name = "첨부파일", description = "의견 요청·기관 회신·일시중단 같은 대상에 붙는 파일")
public class AttachmentController {

  private final AttachmentService attachmentService;

  public AttachmentController(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @GetMapping
  @Operation(summary = "대상 하나의 첨부파일 목록(올린 순)")
  public List<AttachmentResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam AttachmentOwnerType ownerType,
      @RequestParam Long ownerId) {
    return attachmentService.list(user, ownerType, ownerId);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "첨부파일 올리기 (multipart: file)")
  public AttachmentResponse upload(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam AttachmentOwnerType ownerType,
      @RequestParam Long ownerId,
      @RequestPart("file") MultipartFile file) {
    return attachmentService.upload(user, ownerType, ownerId, file);
  }

  @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(summary = "첨부파일 내려받기 (원래 이름으로)")
  public ResponseEntity<Resource> download(
      @AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
    AttachmentDownload download = attachmentService.download(user, id);
    Attachment attachment = download.attachment();
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename(attachment.getOriginalName(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(attachment.getContentType()))
        .contentLength(attachment.getSizeBytes())
        .body(download.content());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "첨부파일 지우기")
  public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long id) {
    attachmentService.delete(user, id);
  }
}
