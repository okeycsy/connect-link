package com.connectlink.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ChangeHistoryAccessPolicyTest {

  /** 관리자만 보는 대상 권한(기관과 같은 규칙). */
  private static final HistoryTargetAccess ADMIN_ONLY =
      new HistoryTargetAccess() {
        @Override
        public HistoryTargetType targetType() {
          return HistoryTargetType.ORGANIZATION;
        }

        @Override
        public boolean canView(CurrentUser user, Long targetId) {
          return user.admin();
        }
      };

  private final ChangeHistoryAccessPolicy policy =
      new ChangeHistoryAccessPolicy(List.of(ADMIN_ONLY));

  @Test
  void 대상_권한_빈이_허용하면_이력을_볼_수_있다() {
    assertThat(policy.canView(TestUsers.ADMIN, HistoryTargetType.ORGANIZATION, 1L)).isTrue();
    policy.ensureCanView(TestUsers.ADMIN, HistoryTargetType.ORGANIZATION, 1L);
  }

  @Test
  void 대상_권한_빈이_거부하면_이력을_볼_수_없다() {
    assertThat(policy.canView(TestUsers.GANA_MEMBER, HistoryTargetType.ORGANIZATION, 1L)).isFalse();
    assertThatThrownBy(
            () -> policy.ensureCanView(TestUsers.GANA_MEMBER, HistoryTargetType.ORGANIZATION, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 권한_빈이_없는_대상은_관리자도_볼_수_없다() {
    assertThat(policy.canView(TestUsers.ADMIN, HistoryTargetType.CONTACT, 1L)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanView(TestUsers.ADMIN, HistoryTargetType.CONTACT, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 같은_대상에_권한_빈이_둘이면_기동이_실패한다() {
    assertThatThrownBy(() -> new ChangeHistoryAccessPolicy(List.of(ADMIN_ONLY, ADMIN_ONLY)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ORGANIZATION");
  }
}
