package com.connectlink.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.organization.Business;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class BusinessAccessPolicyTest {

  private final BusinessAccessPolicy policy = new BusinessAccessPolicy();

  @Test
  void 담당_업무는_볼_수_있다() {
    assertThat(policy.canView(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO)).isTrue();
  }

  @Test
  void 담당이_아닌_업무는_볼_수_없다() {
    assertThat(policy.canView(TestUsers.GANA_MEMBER, Business.CMS)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanView(TestUsers.GANA_MEMBER, Business.CMS))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 관리자는_전_업무를_본다() {
    for (Business business : Business.values()) {
      assertThat(policy.canView(TestUsers.ADMIN, business)).isTrue();
    }
  }
}
