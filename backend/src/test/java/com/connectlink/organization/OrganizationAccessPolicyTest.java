package com.connectlink.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class OrganizationAccessPolicyTest {

  private final OrganizationAccessPolicy policy = new OrganizationAccessPolicy();

  @Test
  void 관리자는_기준정보를_관리할_수_있다() {
    assertThat(policy.canManage(TestUsers.ADMIN)).isTrue();
    policy.ensureCanManage(TestUsers.ADMIN);
  }

  @Test
  void 관리자가_아니면_기준정보를_관리할_수_없다() {
    assertThat(policy.canManage(TestUsers.GANA_MEMBER)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanManage(TestUsers.GANA_MEMBER))
        .isInstanceOf(AccessDeniedException.class);
  }
}
