package com.connectlink.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.attachment.sample.DemoAttachmentOwnerAccess;
import com.connectlink.auth.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AttachmentAccessPolicyTest {

  private final AttachmentAccessPolicy policy =
      new AttachmentAccessPolicy(List.of(new DemoAttachmentOwnerAccess()));

  @Test
  void 대상_권한_빈이_허용하면_보고_올릴_수_있다() {
    assertThat(policy.canView(TestUsers.GANA_MEMBER, AttachmentOwnerType.DEMO, 1L)).isTrue();
    assertThat(policy.canEdit(TestUsers.GANA_MEMBER, AttachmentOwnerType.DEMO, 1L)).isTrue();
    policy.ensureCanView(TestUsers.ADMIN, AttachmentOwnerType.DEMO, 1L);
    policy.ensureCanEdit(TestUsers.ADMIN, AttachmentOwnerType.DEMO, 1L);
  }

  @Test
  void 권한_빈이_없는_대상은_관리자도_쓸_수_없다() {
    assertThat(policy.canView(TestUsers.ADMIN, AttachmentOwnerType.SUSPENSION, 1L)).isFalse();
    assertThat(policy.canEdit(TestUsers.ADMIN, AttachmentOwnerType.SUSPENSION, 1L)).isFalse();
    assertThatThrownBy(
            () -> policy.ensureCanView(TestUsers.ADMIN, AttachmentOwnerType.SUSPENSION, 1L))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () -> policy.ensureCanEdit(TestUsers.ADMIN, AttachmentOwnerType.SUSPENSION, 1L))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 같은_대상에_권한_빈이_둘이면_기동이_실패한다() {
    DemoAttachmentOwnerAccess access = new DemoAttachmentOwnerAccess();
    assertThatThrownBy(() -> new AttachmentAccessPolicy(List.of(access, access)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEMO");
  }
}
