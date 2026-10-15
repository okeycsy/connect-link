package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 계정 상태별 로그인 규칙. 사용 중만 허용하고, 승인 대기·반려는 상태를 알리며 거부하고, 탈퇴는 없는 계정처럼 거부한다. */
class AccountServiceTest {

  private static final String EMAIL = "someone@gana.example.com";
  private static final String PASSWORD = "secret-1234";

  private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
  private final UserAccountRepository repository = mock(UserAccountRepository.class);
  private final AccountService service = new AccountService(repository, encoder);

  private void givenAccount(AccountStatus status) {
    Organization organization = mock(Organization.class);
    when(organization.getCode()).thenReturn("GANA");
    when(organization.getName()).thenReturn("가나은행");
    when(organization.getType()).thenReturn(OrganizationType.FINANCE_ORGANIZATION);
    UserAccount account = mock(UserAccount.class);
    when(account.getEmail()).thenReturn(EMAIL);
    when(account.getName()).thenReturn("가나담당");
    when(account.getPasswordHash()).thenReturn(encoder.encode(PASSWORD));
    when(account.getStatus()).thenReturn(status);
    when(account.getOrganization()).thenReturn(organization);
    when(account.getJobRole()).thenReturn(JobRole.BUSINESS);
    when(account.getBusinesses()).thenReturn(EnumSet.noneOf(Business.class));
    when(account.getItRoles()).thenReturn(EnumSet.noneOf(ItRole.class));
    when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(account));
  }

  private void assertRejected(String password, HttpStatus status, String code) {
    assertThatThrownBy(() -> service.authenticate(EMAIL, password))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> {
              assertThat(e.getStatus()).isEqualTo(status);
              assertThat(e.getCode()).isEqualTo(code);
            });
  }

  @Test
  void 사용_중_계정은_로그인된다() {
    givenAccount(AccountStatus.ACTIVE);
    CurrentUser user = service.authenticate(EMAIL, PASSWORD);
    assertThat(user.email()).isEqualTo(EMAIL);
    assertThat(user.organizationCode()).isEqualTo("GANA");
  }

  @Test
  void 승인_대기_계정은_403_승인_대기다() {
    givenAccount(AccountStatus.PENDING_APPROVAL);
    assertRejected(PASSWORD, HttpStatus.FORBIDDEN, "ACCOUNT_PENDING_APPROVAL");
  }

  @Test
  void 반려된_계정은_403_반려다() {
    givenAccount(AccountStatus.REJECTED);
    assertRejected(PASSWORD, HttpStatus.FORBIDDEN, "ACCOUNT_REJECTED");
  }

  @Test
  void 반려된_계정도_비밀번호가_틀리면_상태를_알리지_않는다() {
    givenAccount(AccountStatus.REJECTED);
    assertRejected("wrong", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
  }

  @Test
  void 탈퇴한_계정은_없는_계정과_같은_401이다() {
    givenAccount(AccountStatus.WITHDRAWN);
    assertRejected(PASSWORD, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
  }

  @Test
  void 없는_계정은_401이다() {
    when(repository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    assertRejected(PASSWORD, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
  }

  @Test
  void 이메일은_소문자로_맞춰_찾는다() {
    givenAccount(AccountStatus.ACTIVE);
    assertThat(service.authenticate("  SomeOne@GANA.example.com", PASSWORD).email())
        .isEqualTo(EMAIL);
  }
}
