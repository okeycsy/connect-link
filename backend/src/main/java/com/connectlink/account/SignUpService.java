package com.connectlink.account;

import com.connectlink.account.dto.SignUpBusinessRequest;
import com.connectlink.account.dto.SignUpRequest;
import com.connectlink.account.dto.SignUpResponse;
import com.connectlink.common.BusinessException;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import com.connectlink.organization.dto.BusinessResponse;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가입 신청. 회사 이메일 인증 토큰을 확인하고 계정을 승인 대기 상태로 만든다. 승인 전에는 로그인할 수 없다({@link
 * AccountService#authenticate}). 결제원 직원과 기관 담당자가 같은 절차를 쓴다(docs/decisions.md). 승인과 승인자 알림은 다음 단계(가입
 * 승인)에서 더한다.
 */
@Service
@Transactional
public class SignUpService {

  private final UserAccountRepository accountRepository;
  private final EmailVerificationService verificationService;
  private final OrganizationService organizationService;
  private final PasswordEncoder passwordEncoder;

  public SignUpService(
      UserAccountRepository accountRepository,
      EmailVerificationService verificationService,
      OrganizationService organizationService,
      PasswordEncoder passwordEncoder) {
    this.accountRepository = accountRepository;
    this.verificationService = verificationService;
    this.organizationService = organizationService;
    this.passwordEncoder = passwordEncoder;
  }

  public SignUpResponse signUp(SignUpRequest request) {
    String email = AccountService.normalizeEmail(request.email());
    verificationService.consume(request.verificationToken(), email);
    verificationService.ensureNotRegistered(email);

    Organization organization = organizationOf(request.organizationId());
    Map<Business, String> businesses = businessesOf(organization, request.businesses());
    Set<ItRole> itRoles = itRolesOf(request.jobRole(), request.itRoles());

    UserAccount account =
        accountRepository.save(
            UserAccount.apply(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                organization,
                request.jobRole(),
                itRoles,
                businesses,
                request.department().trim(),
                request.team().trim(),
                request.jobTitle().trim(),
                request.extensionPhone().trim(),
                blankToNull(request.mobilePhone())));
    return toResponse(account);
  }

  private Organization organizationOf(Long organizationId) {
    Organization organization;
    try {
      organization = organizationService.getActive(organizationId);
    } catch (BusinessException e) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, e.getCode(), "organizationId", e.getMessage());
    }
    // 이용기관 담당자의 권한과 기능은 명세가 정하기 전까지 만들지 않는다(CLAUDE.md 용어집).
    if (organization.getType() == OrganizationType.USE_ORGANIZATION) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "ORGANIZATION_NOT_ALLOWED",
          "organizationId",
          "이용기관 담당자 가입은 아직 받지 않습니다.");
    }
    return organization;
  }

  /** 고른 업무는 기관이 참여하는 업무여야 한다. 같은 업무를 두 번 보내면 하나로 친다. 세부업무는 앞뒤 공백을 떼고, 비면 없다. */
  private static Map<Business, String> businessesOf(
      Organization organization, List<SignUpBusinessRequest> requested) {
    Map<Business, String> result = new EnumMap<>(Business.class);
    for (SignUpBusinessRequest item : requested) {
      if (!organization.participatesIn(item.business())) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "BUSINESS_NOT_PARTICIPATED",
            "businesses",
            organization.getName() + " 은(는) " + item.business().getLabel() + " 에 참여하지 않는 기관입니다.");
      }
      result.put(item.business(), blankToNull(item.subBusiness()));
    }
    return result;
  }

  /** 전산이면 세부 직무를 하나 이상 골라야 하고, 현업이면 세부 직무를 버린다. */
  private static Set<ItRole> itRolesOf(JobRole jobRole, List<ItRole> requested) {
    if (jobRole != JobRole.IT) {
      return EnumSet.noneOf(ItRole.class);
    }
    if (requested == null || requested.isEmpty()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "IT_ROLE_REQUIRED", "itRoles", "전산 세부 직무를 하나 이상 골라 주세요.");
    }
    return EnumSet.copyOf(requested);
  }

  private static SignUpResponse toResponse(UserAccount account) {
    Organization organization = account.getOrganization();
    return new SignUpResponse(
        account.getEmail(),
        account.getName(),
        organization.getName(),
        organization.getType().getLabel(),
        account.getBusinesses().stream().map(BusinessResponse::from).toList(),
        account.getJobRole().describe(account.getItRoles()),
        account.getStatus(),
        account.getStatus().getLabel(),
        account.getCreatedAt());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
