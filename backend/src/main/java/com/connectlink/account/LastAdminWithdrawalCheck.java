package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import java.util.List;
import org.springframework.stereotype.Component;

/** 사용 중인 포털 관리자가 한 명뿐이면 그 관리자는 탈퇴하지 못한다. 기준정보를 관리할 사람이 없어지기 때문이다. */
@Component
class LastAdminWithdrawalCheck implements WithdrawalCheck {

  private final UserAccountRepository repository;

  LastAdminWithdrawalCheck(UserAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<WithdrawalBlocker> check(CurrentUser user) {
    if (!user.admin() || repository.countByAdminTrueAndStatus(AccountStatus.ACTIVE) > 1) {
      return List.of();
    }
    return List.of(
        new WithdrawalBlocker(
            "LAST_ADMIN", "포털 관리자가 한 명뿐이라 탈퇴할 수 없습니다. 다른 관리자가 생긴 뒤에 탈퇴해 주세요.", null));
  }
}
