package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.organization.Organization;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정. 로그인 확인과 로그인 사용자({@link CurrentUser}) 만들기를 맡는다. 가입 신청은 {@link SignUpService}, 회사 이메일 인증은
 * {@link EmailVerificationService} 가 한다. 가입 승인·탈퇴는 다음 단계에서 더한다.
 */
@Service
@Transactional(readOnly = true)
public class AccountService {

  /** 없는 이메일에도 비밀번호 비교를 한 번 해서 응답 시간으로 계정 유무가 드러나지 않게 한다. 아무 비밀번호와도 맞지 않는 BCrypt 형식의 값이다. */
  private static final String NO_ACCOUNT_HASH =
      "$2a$10$K26FVsN1Lr5pMYurY.z9BeV.ZF4zVqcZ8cVq4kRkVL7wsZJjMaUXS";

  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;

  public AccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * 회사 이메일과 비밀번호를 확인하고 로그인 사용자를 돌려준다. 계정이 없거나 비밀번호가 틀리면 같은 401 이고, 비밀번호가 맞을 때만 승인 대기·반려 같은 상태를 알려
   * 준다.
   */
  public CurrentUser authenticate(String email, String password) {
    UserAccount account = repository.findByEmail(normalizeEmail(email)).orElse(null);
    boolean matches =
        passwordEncoder.matches(
            password, account == null ? NO_ACCOUNT_HASH : account.getPasswordHash());
    if (account == null || !matches || account.getStatus() == AccountStatus.WITHDRAWN) {
      throw new BusinessException(
          HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 맞지 않습니다.");
    }
    switch (account.getStatus()) {
      case PENDING_APPROVAL ->
          throw new BusinessException(
              HttpStatus.FORBIDDEN,
              "ACCOUNT_PENDING_APPROVAL",
              "가입 승인 전입니다. 결제원 담당자가 승인하면 회사 이메일로 알려 드립니다.");
      case REJECTED ->
          throw new BusinessException(
              HttpStatus.FORBIDDEN, "ACCOUNT_REJECTED", "가입이 반려된 계정입니다. 결제원 담당자에게 문의해 주세요.");
      default -> {
        // ACTIVE
      }
    }
    return toCurrentUser(account);
  }

  /** 사용 중인 계정 전부. 관리자, 결제원, 참가기관, 이용기관 순이고 같은 구분 안에서는 이름순이다. */
  public List<CurrentUser> listActive() {
    return repository.findByStatus(AccountStatus.ACTIVE).stream()
        .sorted(
            Comparator.comparing((UserAccount a) -> !a.isAdmin())
                .thenComparing(a -> a.getOrganization().getType().ordinal())
                .thenComparing(UserAccount::getName))
        .map(AccountService::toCurrentUser)
        .toList();
  }

  static CurrentUser toCurrentUser(UserAccount account) {
    Organization organization = account.getOrganization();
    return new CurrentUser(
        account.getEmail(),
        account.getName(),
        organization.getCode(),
        organization.getName(),
        organization.getType(),
        account.isAdmin(),
        account.getBusinesses(),
        account.getJobRole(),
        account.getItRoles());
  }

  /** 이메일은 앞뒤 공백을 떼고 소문자로 비교·저장한다. */
  static String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
