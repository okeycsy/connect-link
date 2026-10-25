package com.connectlink.contact;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.contact.dto.ContactCountsResponse;
import com.connectlink.contact.dto.ContactImportResponse;
import com.connectlink.contact.dto.ContactOrganizationConfirmRequest;
import com.connectlink.contact.dto.ContactOrganizationConfirmResponse;
import com.connectlink.contact.dto.ContactOrganizationResponse;
import com.connectlink.contact.dto.ContactResponse;
import com.connectlink.contact.dto.ContactSaveRequest;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 가. 담당자 연락처: 목록·상세, 등록·수정·삭제, "변동 없음" 확인, 기관 현행화 완료, 내 연락처, 로그인 확인 알림, 엑셀. */
@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "담당자 연락처", description = "참가기관·결제원 담당자 연락처 조회·수정과 최근 확인일")
public class ContactController {

  private final ContactService contactService;
  private final ContactImportService importService;
  private final CurrentUserProvider currentUserProvider;

  public ContactController(
      ContactService contactService,
      ContactImportService importService,
      CurrentUserProvider currentUserProvider) {
    this.contactService = contactService;
    this.importService = importService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "담당자 목록 조회",
      description =
          "한 업무의 담당자. 결제원 담당자와 결제원 운영·상황실·대외계는 전 기관을, 기관 담당자는 결제원 담당자와 자기 기관만 본다. 결제원이 맨 위, 기관 이름순, 주담당 → 부담당이다. 검색어·기관·직무·확인 필요 조건은 내부망 전용이라 인터넷망에서 쓰면 403 INTERNAL_ONLY.")
  public PageResponse<ContactResponse> list(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @Parameter(description = "이름, 기관, 부서, 세부업무, 이메일 검색어(내부망)") @RequestParam(required = false)
          String q,
      @Parameter(description = "이 기관만(내부망)") @RequestParam(required = false) Long organizationId,
      @Parameter(description = "이 직무만(내부망)") @RequestParam(required = false) JobRole jobRole,
      @Parameter(description = "최근 확인일 6개월 경과만(내부망)") @RequestParam(defaultValue = "false")
          boolean needsConfirm,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return contactService.list(
        currentUserProvider.require(),
        resolveBusiness(business),
        new ContactService.Filter(q, organizationId, jobRole, needsConfirm),
        PageRequest.of(page, Math.min(size, 200)));
  }

  @GetMapping("/counts")
  @Operation(summary = "담당자 건수 조회", description = "볼 수 있는 범위의 전체 담당자 수와 최근 확인일 6개월 경과 수.")
  public ContactCountsResponse counts(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return contactService.counts(currentUserProvider.require(), resolveBusiness(business));
  }

  @GetMapping("/organizations")
  @Operation(
      summary = "담당자 기관 조회",
      description = "등록 양식과 기관 거르기에 쓰는 기관. 전 기관을 보는 사람은 결제원과 그 업무 참가기관 전부, 기관 담당자는 결제원과 자기 기관이다.")
  public List<ContactOrganizationResponse> organizations(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    return contactService.organizations(currentUserProvider.require(), resolveBusiness(business));
  }

  @GetMapping("/me")
  @Operation(summary = "내 연락처 조회", description = "로그인한 사람 본인의 담당자 행(맡은 업무마다 하나). 내 정보 화면이 쓴다.")
  public List<ContactResponse> mine() {
    return contactService.mine(currentUserProvider.require());
  }

  @GetMapping("/reconfirm-due")
  @Operation(
      summary = "확인이 필요한 담당자 조회(로그인 알림)",
      description =
          "최근 확인일에서 6개월이 지난 담당자 중 로그인한 사람이 확인할 것. 본인 행과, 기관 담당자면 자기 기관의 맡은 업무 담당자다. 화면은 로그인 뒤 한 번 알림을 띄운다.")
  public List<ContactResponse> reconfirmDue() {
    return contactService.reconfirmDue(currentUserProvider.require());
  }

  @GetMapping(value = "/excel", produces = ContactExcel.CONTENT_TYPE)
  @Operation(
      summary = "담당자 목록 엑셀 다운로드",
      description = "목록과 같은 범위·조건의 xlsx. 내부망 전용이라 인터넷망이면 403 INTERNAL_ONLY.")
  public ResponseEntity<byte[]> excel(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long organizationId,
      @RequestParam(required = false) JobRole jobRole,
      @RequestParam(defaultValue = "false") boolean needsConfirm) {
    ContactExcel excel =
        contactService.excel(
            currentUserProvider.require(),
            resolveBusiness(business),
            new ContactService.Filter(q, organizationId, jobRole, needsConfirm));
    ContentDisposition disposition =
        ContentDisposition.attachment().filename(excel.fileName(), StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(ContactExcel.CONTENT_TYPE))
        .body(excel.content());
  }

  @GetMapping(value = "/import-template", produces = ContactExcel.CONTENT_TYPE)
  @Operation(
      summary = "담당자 일괄 등록 양식 내려받기",
      description = "머리글·예시 줄·적는 법 시트가 있는 xlsx. 그 업무 결제원 담당자만, 내부망에서만(403 INTERNAL_ONLY).")
  public ResponseEntity<byte[]> importTemplate(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business) {
    Business resolved = resolveBusiness(business);
    byte[] content = importService.template(currentUserProvider.require(), resolved);
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename("담당자일괄등록양식_" + resolved.getLabel() + ".xlsx", StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(ContactExcel.CONTENT_TYPE))
        .body(content);
  }

  @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "담당자 일괄 등록 (multipart: file)",
      description =
          "채운 양식을 줄마다 화면 등록과 같은 규칙으로 검사한다. dryRun 이면 검사만 하고, 아니면 오류가 없을 때만 등록한다(한 줄이라도 오류면 아무것도 등록하지"
              + " 않는다). 같은 기관·업무에 같은 이메일이 있으면 건너뛴다. 읽지 못하는 파일이면 400 CONTACT_IMPORT_INVALID_FILE.")
  public ContactImportResponse importContacts(
      @Parameter(description = "업무 영역 slug (예: account-info)") @RequestParam String business,
      @RequestParam(defaultValue = "false") boolean dryRun,
      @RequestPart("file") MultipartFile file)
      throws IOException {
    try (InputStream in = file.getInputStream()) {
      return importService.importContacts(
          currentUserProvider.require(), resolveBusiness(business), in, dryRun);
    }
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "담당자 등록",
      description =
          "결제원 담당자는 결제원이나 그 업무 참가기관을 골라(내부망), 기관 담당자는 자기 기관에 후임자 등을 등록한다. 같은 기관·업무에 같은 이메일이 있으면 409 CONTACT_DUPLICATE.")
  public ContactResponse create(@Valid @RequestBody ContactSaveRequest request) {
    return contactService.create(currentUserProvider.require(), request);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "담당자 상세 조회",
      description = "permissions 가 지금 할 수 있는 일이다. 변경 이력은 변경 이력 API 로 본다.")
  public ContactResponse get(@PathVariable Long id) {
    return contactService.get(currentUserProvider.require(), id);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "담당자 수정",
      description =
          "본인과 소속 기관의 같은 업무 담당자, 또는 그 업무 결제원 담당자(내부망)가 고친다. 저장하면 최근 확인일이 갱신되고 바뀐 항목은 변경 전·후 이력으로 남는다. 기관과 업무는 바뀌지 않는다.")
  public ContactResponse update(
      @PathVariable Long id, @Valid @RequestBody ContactSaveRequest request) {
    return contactService.update(currentUserProvider.require(), id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "담당자 삭제", description = "그 업무 결제원 담당자가 내부망에서 지운다. 삭제 이력이 남는다.")
  public void delete(@PathVariable Long id) {
    contactService.delete(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/confirmation")
  @Operation(summary = "변동 없음 확인", description = "내용은 그대로 두고 최근 확인일만 오늘로 한다. 고칠 수 있는 사람이 한다.")
  public ContactResponse confirm(@PathVariable Long id) {
    return contactService.confirm(currentUserProvider.require(), id);
  }

  @PostMapping("/organization-confirmations")
  @Operation(
      summary = "기관 담당자 현행화 완료",
      description =
          "기관 하나의 한 업무 담당자 전부를 변동 없음으로 확인한다. 정기 변경 주간이 진행 중이면 그 기관을 현행화 완료로 남긴다. 기관 담당자는 자기 기관, 결제원 담당자는 고른 기관(내부망). 담당자가 없으면 409 CONTACT_NONE.")
  public ContactOrganizationConfirmResponse confirmOrganization(
      @Valid @RequestBody ContactOrganizationConfirmRequest request) {
    return contactService.confirmOrganization(currentUserProvider.require(), request);
  }

  static Business resolveBusiness(String slug) {
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
