package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class WithdrawalAccessPolicyTest {

  private final WithdrawalAccessPolicy policy = new WithdrawalAccessPolicy();

  private static UserAccount account(OrganizationType type, Business business) {
    Organization organization =
        new Organization(type == OrganizationType.KFTC ? "KFTC" : "GANA", "기관", type, null);
    return UserAccount.apply(
        "someone@gana.example.com",
        "hash",
        "누군가",
        organization,
        JobRole.BUSINESS,
        Set.of(),
        Map.of(business, "세부업무"),
        "부서",
        "팀",
        "대리",
        "1234",
        null,
        LocalDateTime.now());
  }

  @Test
  void 그_업무를_맡은_결제원_담당자는_참가기관_담당자를_탈퇴_처리한다() {
    assertThat(
            policy.canWithdraw(
                TestUsers.KFTC_ACCOUNT_INFO,
                account(OrganizationType.FINANCE_ORGANIZATION, Business.AUTO_TRANSFER)))
        .isTrue();
  }

  @Test
  void 다른_업무만_맡은_결제원_담당자는_탈퇴_처리하지_못한다() {
    UserAccount target = account(OrganizationType.FINANCE_ORGANIZATION, Business.ACCOUNT_INFO);
    assertThat(policy.canWithdraw(TestUsers.KFTC_CMS, target)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanWithdraw(TestUsers.KFTC_CMS, target))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 관리자는_업무와_상관없이_참가기관_담당자를_탈퇴_처리한다() {
    assertThat(
            policy.canWithdraw(
                TestUsers.ADMIN, account(OrganizationType.FINANCE_ORGANIZATION, Business.CMS)))
        .isTrue();
  }

  @Test
  void 참가기관_담당자는_같은_업무여도_탈퇴_처리하지_못한다() {
    assertThat(
            policy.canWithdraw(
                TestUsers.GANA_MEMBER,
                account(OrganizationType.FINANCE_ORGANIZATION, Business.ACCOUNT_INFO)))
        .isFalse();
  }

  @Test
  void 결제원_계정은_관리자도_탈퇴_처리하지_못한다() {
    assertThat(policy.canWithdraw(TestUsers.ADMIN, account(OrganizationType.KFTC, Business.CMS)))
        .isFalse();
  }
}
