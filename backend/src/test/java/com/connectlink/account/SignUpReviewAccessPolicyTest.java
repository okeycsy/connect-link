package com.connectlink.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SignUpReviewAccessPolicyTest {

  private final SignUpReviewAccessPolicy policy = new SignUpReviewAccessPolicy();

  /** 담당 업무에 CMS 가 있는 참가기관 담당자. 업무가 같아도 결제원이 아니면 승인하지 못한다. */
  private static final CurrentUser FINANCE_CMS_MEMBER =
      new CurrentUser(
          "lee.dara@dara.example.com",
          "이다라",
          "DARA",
          "다라은행",
          OrganizationType.FINANCE_ORGANIZATION,
          false,
          Set.of(Business.CMS),
          JobRole.BUSINESS,
          Set.of());

  @Test
  void 관리자는_모든_업무의_신청을_처리한다() {
    for (Business business : Business.values()) {
      assertThat(policy.canReview(TestUsers.ADMIN, business)).isTrue();
    }
    policy.ensureCanReview(TestUsers.ADMIN, Set.of(Business.LOAN_TRANSFER));
  }

  @Test
  void 결제원_담당자는_맡은_업무의_신청을_처리한다() {
    assertThat(policy.canReview(TestUsers.KFTC_CMS, Business.CMS)).isTrue();
    policy.ensureCanReview(TestUsers.KFTC_CMS, Business.CMS);
  }

  @Test
  void 결제원_담당자도_맡지_않은_업무의_신청은_처리하지_못한다() {
    assertThat(policy.canReview(TestUsers.KFTC_CMS, Business.ACCOUNT_INFO)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanReview(TestUsers.KFTC_CMS, Business.ACCOUNT_INFO))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 신청한_업무_중_하나만_맡아도_신청_전체를_처리한다() {
    assertThat(policy.canReview(TestUsers.KFTC_CMS, Set.of(Business.ACCOUNT_INFO, Business.CMS)))
        .isTrue();
  }

  @Test
  void 신청한_업무를_하나도_맡지_않으면_처리하지_못한다() {
    Set<Business> requested = Set.of(Business.ACCOUNT_INFO, Business.LOAN_TRANSFER);
    assertThat(policy.canReview(TestUsers.KFTC_CMS, requested)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanReview(TestUsers.KFTC_CMS, requested))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 기관_담당자는_같은_업무라도_처리하지_못한다() {
    assertThat(policy.canReview(FINANCE_CMS_MEMBER, Business.CMS)).isFalse();
    assertThat(policy.canReview(TestUsers.GANA_MEMBER, Set.of(Business.ACCOUNT_INFO))).isFalse();
  }
}
