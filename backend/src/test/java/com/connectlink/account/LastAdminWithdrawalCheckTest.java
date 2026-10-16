package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.connectlink.auth.TestUsers;
import org.junit.jupiter.api.Test;

class LastAdminWithdrawalCheckTest {

  private final UserAccountRepository repository = mock(UserAccountRepository.class);
  private final LastAdminWithdrawalCheck check = new LastAdminWithdrawalCheck(repository);

  @Test
  void 사용_중_관리자가_한_명뿐이면_그_관리자를_막는다() {
    when(repository.countByAdminTrueAndStatus(AccountStatus.ACTIVE)).thenReturn(1L);
    assertThat(check.check(TestUsers.ADMIN))
        .extracting(WithdrawalBlocker::code)
        .containsExactly("LAST_ADMIN");
  }

  @Test
  void 관리자가_두_명_이상이면_관리자도_탈퇴할_수_있다() {
    when(repository.countByAdminTrueAndStatus(AccountStatus.ACTIVE)).thenReturn(2L);
    assertThat(check.check(TestUsers.ADMIN)).isEmpty();
  }

  @Test
  void 관리자가_아니면_관리자_수와_상관없다() {
    assertThat(check.check(TestUsers.GANA_MEMBER)).isEmpty();
    verifyNoInteractions(repository);
  }
}
