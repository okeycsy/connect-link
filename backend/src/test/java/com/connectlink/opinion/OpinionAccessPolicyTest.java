package com.connectlink.opinion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** 의견 요청 권한 규칙마다 허용과 거부를 하나씩 본다. */
class OpinionAccessPolicyTest {

  private final OpinionAccessPolicy policy = new OpinionAccessPolicy();

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

  /** 가나은행이 대상인 계좌통합관리 요청. {@code open} 이면 회신중이다. */
  private static OpinionRequest requestForGana(boolean open) {
    OpinionRequest request =
        OpinionRequest.draft(
            Business.ACCOUNT_INFO,
            "요청",
            "내용",
            LocalDate.now().plusDays(3),
            "hong.gildong@kftc.example.com",
            "홍길동");
    request.addTarget(new Organization("004", "가나은행", OrganizationType.FINANCE_ORGANIZATION, null));
    if (open) {
      request.open(LocalDateTime.now());
    }
    return request;
  }

  @Test
  void 그_업무의_결제원_담당자는_요청을_관리한다() {
    assertThat(policy.canManage(TestUsers.KFTC_ACCOUNT_INFO, Business.ACCOUNT_INFO)).isTrue();
    assertThat(policy.canManage(TestUsers.ADMIN, Business.CMS)).isTrue();
  }

  @Test
  void 다른_업무의_결제원_담당자와_기관_담당자는_관리하지_못한다() {
    assertThat(policy.canManage(TestUsers.KFTC_CMS, Business.ACCOUNT_INFO)).isFalse();
    assertThat(policy.canManage(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanManage(TestUsers.KFTC_CMS, Business.ACCOUNT_INFO))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 대상_기관_담당자는_회신중_요청을_보고_회신한다() {
    OpinionRequest request = requestForGana(true);
    assertThat(policy.isTargetMember(TestUsers.GANA_MEMBER, request)).isTrue();
    assertThat(policy.canView(TestUsers.GANA_MEMBER, request)).isTrue();
  }

  @Test
  void 대상이_아닌_기관_담당자는_요청을_보지_못한다() {
    OpinionRequest request = requestForGana(true);
    assertThat(policy.canView(TestUsers.SAA_MEMBER, request)).isFalse();
    assertThatThrownBy(() -> policy.ensureTargetMember(TestUsers.SAA_MEMBER, request))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 작성중_요청은_대상_기관에도_보이지_않는다() {
    OpinionRequest draft = requestForGana(false);
    assertThat(policy.canView(TestUsers.GANA_MEMBER, draft)).isFalse();
    assertThat(policy.canView(TestUsers.KFTC_ACCOUNT_INFO, draft)).isTrue();
  }

  @Test
  void 이용기관_담당자는_회신함을_쓰지_않는다() {
    assertThat(policy.isReplier(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO)).isTrue();
    assertThat(policy.isReplier(HAHA_MEMBER, Business.ACCOUNT_INFO)).isFalse();
  }
}
