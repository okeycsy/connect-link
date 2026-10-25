package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.NetworkZoneResolver;
import com.connectlink.contact.dto.ContactImportResponse;
import com.connectlink.contact.dto.ContactImportRowResponse;
import com.connectlink.contact.dto.ContactSaveRequest;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.dto.OrganizationResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당자 일괄 등록(엑셀). 결제원 담당자가 기존 담당자 명부를 한 번에 옮길 때 쓴다. 양식 파일을 내려받아 채워 올리면 줄마다 화면 등록과 같은 규칙으로 검사하고, 한
 * 줄이라도 오류면 아무것도 등록하지 않는다. 같은 기관·업무에 같은 회사 이메일이 이미 있으면 그 줄은 건너뛴다. 그 업무 결제원 담당자만, 내부망에서만 한다.
 */
@Service
@Transactional
public class ContactImportService {

  private final ContactService contactService;
  private final ContactAccessPolicy accessPolicy;
  private final OrganizationService organizationService;
  private final NetworkZoneResolver networkZoneResolver;
  private final Validator validator;

  public ContactImportService(
      ContactService contactService,
      ContactAccessPolicy accessPolicy,
      OrganizationService organizationService,
      NetworkZoneResolver networkZoneResolver,
      Validator validator) {
    this.contactService = contactService;
    this.accessPolicy = accessPolicy;
    this.organizationService = organizationService;
    this.networkZoneResolver = networkZoneResolver;
    this.validator = validator;
  }

  /** 일괄 등록 양식 파일. 그 업무 결제원 담당자만, 내부망에서만 내려받는다. */
  @Transactional(readOnly = true)
  public byte[] template(CurrentUser user, Business business) {
    ensureCanImport(user, business);
    return ContactImportSheet.template(business);
  }

  /**
   * 채운 양식을 검사하고, 검사만 하는 게 아니고 오류가 없으면 등록한다. 엑셀로 읽지 못하거나 머리글이 양식과 다르면 400 {@code
   * CONTACT_IMPORT_INVALID_FILE} 이다.
   *
   * @param dryRun true 면 검사만 한다(화면의 "검사" 단계)
   */
  public ContactImportResponse importContacts(
      CurrentUser user, Business business, InputStream file, boolean dryRun) {
    ensureCanImport(user, business);
    List<ContactImportSheet.Parsed> parsed;
    try {
      parsed = ContactImportSheet.read(file, business);
    } catch (IllegalArgumentException e) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "CONTACT_IMPORT_INVALID_FILE", "file", e.getMessage());
    }
    // 기관은 사용 중인 기관에서만 찾는다. 없는 코드로 예외를 던지는 조회를 부르면 트랜잭션이 되돌림으로 표시돼 등록까지 실패한다.
    Map<String, Long> activeIds =
        organizationService.listActive().stream()
            .collect(Collectors.toMap(OrganizationResponse::code, OrganizationResponse::id));
    List<ContactImportRowResponse> rows = new ArrayList<>();
    List<ContactSaveRequest> creatable = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (ContactImportSheet.Parsed row : parsed) {
      Organization organization = findOrganization(activeIds, row.organizationCode());
      String orgName = organization == null ? null : organization.getName();
      ContactSaveRequest request = row.request();
      String name = request == null ? null : request.name();
      String email = request == null ? null : request.email().trim().toLowerCase(Locale.ROOT);
      String error = row.error();
      if (error == null && organization == null) {
        error = "기관 코드 '" + row.organizationCode() + "' 인 사용 중 기관이 없습니다.";
      }
      ContactSaveRequest withOrganization =
          error == null ? withOrganization(request, organization.getId()) : null;
      if (error == null) {
        error = violations(withOrganization);
      }
      boolean exists = false;
      if (error == null) {
        try {
          exists = contactService.checkImportable(user, withOrganization);
        } catch (RuntimeException e) {
          error = e.getMessage();
        }
      }
      if (error == null && !seen.add(organization.getCode() + "|" + email)) {
        error = "파일 안에 같은 기관·회사 이메일 줄이 또 있습니다.";
      }
      ContactImportRowStatus status =
          error != null
              ? ContactImportRowStatus.ERROR
              : exists ? ContactImportRowStatus.SKIP : ContactImportRowStatus.CREATE;
      if (status == ContactImportRowStatus.CREATE) {
        creatable.add(withOrganization);
      }
      rows.add(
          new ContactImportRowResponse(
              row.rowNumber(),
              row.organizationCode(),
              orgName,
              name,
              email,
              status,
              status.getLabel(),
              status == ContactImportRowStatus.SKIP ? "같은 회사 이메일이 이미 등록돼 있어 건너뜁니다." : error));
    }
    int errors =
        (int) rows.stream().filter(r -> r.status() == ContactImportRowStatus.ERROR).count();
    int skips = (int) rows.stream().filter(r -> r.status() == ContactImportRowStatus.SKIP).count();
    boolean save = !dryRun && errors == 0 && !creatable.isEmpty();
    if (save) {
      creatable.forEach(request -> contactService.create(user, request));
    }
    rows.sort(Comparator.comparingInt(ContactImportRowResponse::rowNumber));
    return new ContactImportResponse(rows.size(), creatable.size(), skips, errors, save, rows);
  }

  private void ensureCanImport(CurrentUser user, Business business) {
    if (!accessPolicy.canManage(user, business)) {
      throw new AccessDeniedException("그 업무를 맡은 결제원 담당자만 담당자를 일괄 등록합니다.");
    }
    networkZoneResolver.ensureInternal("담당자 일괄 등록");
  }

  private Organization findOrganization(Map<String, Long> activeIds, String code) {
    Long id = code == null ? null : activeIds.get(code.trim().toUpperCase(Locale.ROOT));
    return id == null ? null : organizationService.getActive(id);
  }

  private String violations(ContactSaveRequest request) {
    Set<ConstraintViolation<ContactSaveRequest>> violations = validator.validate(request);
    return violations.isEmpty()
        ? null
        : violations.stream()
            .map(ConstraintViolation::getMessage)
            .sorted()
            .collect(Collectors.joining(" "));
  }

  private static ContactSaveRequest withOrganization(ContactSaveRequest r, Long organizationId) {
    return new ContactSaveRequest(
        r.business(),
        organizationId,
        r.subBusiness(),
        r.name(),
        r.department(),
        r.team(),
        r.jobTitle(),
        r.jobRole(),
        r.itRoles(),
        r.rank(),
        r.extensionPhone(),
        r.mobilePhone(),
        r.email());
  }
}
