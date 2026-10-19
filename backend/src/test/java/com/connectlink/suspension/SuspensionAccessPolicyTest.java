package com.connectlink.suspension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** 일시중단 권한 규칙마다 허용과 거부를 하나씩 본다. */
class SuspensionAccessPolicyTest {

  private final SuspensionAccessPolicy policy = new SuspensionAccessPolicy();

  /** KB국민은행의 다른 계좌통합관리 담당자(같은 기관, 등록한 사람 아님). */
  private static final CurrentUser GANA_COLLEAGUE =
      new CurrentUser(
          "han.gana@kb.example.com",
          "한가나",
          "004",
          "KB국민은행",
          OrganizationType.FINANCE_ORGANIZATION,
          false,
          Set.of(Business.ACCOUNT_INFO),
          JobRole.BUSINESS,
          Set.of());

  private static final CurrentUser HAHA_MEMBER =
      new CurrentUser(
          "oh.haha@haha.example.com",
          "오하하",
          "HAHA",
          "하하통신",
          OrganizationType.USE_ORGANIZATION,
          false,
          Set.of(Business.ACCOUNT_INFO),
          JobRole.BUSINESS,
          Set.of());

  /** 김가나가 올린 KB국민은행 계좌통합관리 일시중단. */
  private static Suspension ganaSuspension() {
    LocalDateTime start = LocalDateTime.now().plusDays(1);
    return Suspension.register(
        new Organization("004", "KB국민은행", OrganizationType.FINANCE_ORGANIZATION, null),
        new Suspension.Content(
            Business.ACCOUNT_INFO,
            null,
            start,
            start.plusHours(2),
            "점검",
            null,
            "김가나",
            "010-0000-2101",
            null,
            null),
        "kim.gana@kb.example.com",
        "김가나",
        false);
  }

  @Test
  void 그_업무의_결제원_담당자와_관리자는_결제원으로_다룬다() {
    assertThat(policy.canManage(TestUsers.KFTC_ACCOUNT_INFO, Business.ACCOUNT_INFO)).isTrue();
    assertThat(policy.canManage(TestUsers.ADMIN, Business.CMS)).isTrue();
  }

  @Test
  void 다른_업무의_결제원과_기관_담당자는_결제원으로_다루지_못한다() {
    assertThat(policy.canManage(TestUsers.KFTC_CMS, Business.ACCOUNT_INFO)).isFalse();
    assertThat(policy.canManage(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO)).isFalse();
  }

  @Test
  void 참가기관_담당자는_맡은_업무에_등록한다() {
    assertThat(policy.canRegister(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO)).isTrue();
  }

  @Test
  void 맡지_않은_업무와_이용기관은_등록하지_못한다() {
    assertThat(policy.canRegister(TestUsers.GANA_MEMBER, Business.CMS)).isFalse();
    assertThat(policy.canRegister(HAHA_MEMBER, Business.ACCOUNT_INFO)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanRegister(HAHA_MEMBER, Business.ACCOUNT_INFO))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 결제원_담당자와_같은_기관_담당자는_본다() {
    Suspension s = ganaSuspension();
    assertThat(policy.canView(TestUsers.KFTC_ACCOUNT_INFO, s)).isTrue();
    assertThat(policy.canView(GANA_COLLEAGUE, s)).isTrue();
  }

  @Test
  void 다른_기관과_다른_업무의_결제원은_보지_못한다() {
    Suspension s = ganaSuspension();
    assertThat(policy.canView(TestUsers.SAA_MEMBER, s)).isFalse();
    assertThat(policy.canView(TestUsers.KFTC_CMS, s)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanView(TestUsers.SAA_MEMBER, s))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 등록한_기관_담당자와_결제원_담당자는_고친다() {
    Suspension s = ganaSuspension();
    assertThat(policy.canEdit(TestUsers.GANA_MEMBER, s)).isTrue();
    assertThat(policy.canEdit(TestUsers.KFTC_ACCOUNT_INFO, s)).isTrue();
  }

  @Test
  void 같은_기관이라도_본인이_올린_것이_아니면_고치지_못한다() {
    Suspension s = ganaSuspension();
    assertThat(policy.canEdit(GANA_COLLEAGUE, s)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanEdit(GANA_COLLEAGUE, s))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 등록한_본인은_지운다() {
    assertThat(policy.canDelete(TestUsers.GANA_MEMBER, ganaSuspension())).isTrue();
  }

  @Test
  void 결제원_담당자도_남이_올린_것은_지우지_못한다() {
    Suspension s = ganaSuspension();
    assertThat(policy.canDelete(TestUsers.KFTC_ACCOUNT_INFO, s)).isFalse();
    assertThat(policy.canDelete(GANA_COLLEAGUE, s)).isFalse();
  }

  @Test
  void 결제원_담당자는_확인하고_확정한다() {
    assertThat(policy.canReview(TestUsers.KFTC_ACCOUNT_INFO, ganaSuspension())).isTrue();
  }

  @Test
  void 기관_담당자는_확인하거나_확정하지_못한다() {
    Suspension s = ganaSuspension();
    assertThat(policy.canReview(TestUsers.GANA_MEMBER, s)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanReview(TestUsers.GANA_MEMBER, s))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 완료한_건은_아무도_고치거나_지우거나_확정하지_못한다() {
    Suspension s = ganaSuspension();
    LocalDateTime now = LocalDateTime.now();
    s.confirm("차단", "hong.gildong@kftc.example.com", "홍길동", "현업", now);
    s.complete(now);
    assertThat(policy.canEdit(TestUsers.KFTC_ACCOUNT_INFO, s)).isFalse();
    assertThat(policy.canDelete(TestUsers.GANA_MEMBER, s)).isFalse();
    assertThat(policy.canReview(TestUsers.KFTC_ACCOUNT_INFO, s)).isFalse();
    assertThat(policy.canView(TestUsers.GANA_MEMBER, s)).isTrue();
  }

  @Test
  void 확인을_시작하면_검토_중_다시_검토하면_검토_대기다() {
    Suspension s = ganaSuspension();
    LocalDateTime now = LocalDateTime.now();
    assertThat(s.getStatus()).isEqualTo(SuspensionStatus.PENDING_REVIEW);
    assertThat(s.startReview("kim.ops@kftc.example.com", "김운영", "전산(운영)", now)).isTrue();
    assertThat(s.startReview("kim.ops@kftc.example.com", "김운영", "전산(운영)", now)).isFalse();
    assertThat(s.getStatus()).isEqualTo(SuspensionStatus.IN_REVIEW);
    s.reopenReview();
    assertThat(s.getStatus()).isEqualTo(SuspensionStatus.PENDING_REVIEW);
    assertThat(s.getReviewers()).isEmpty();
  }

  @Test
  void 결제원_담당자와_관리자는_전_영업일_취합을_수동으로_보낸다() {
    assertThat(policy.canSendDigest(TestUsers.KFTC_CMS)).isTrue();
    assertThat(policy.canSendDigest(TestUsers.ADMIN)).isTrue();
  }

  @Test
  void 기관_담당자는_전_영업일_취합을_보내지_못한다() {
    assertThat(policy.canSendDigest(TestUsers.GANA_MEMBER)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanSendDigest(TestUsers.GANA_MEMBER))
        .isInstanceOf(AccessDeniedException.class);
  }
}
