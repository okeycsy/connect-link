package com.connectlink.testplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** 기관 테스트 권한 규칙마다 허용과 거부를 하나씩 본다. */
class TestPlanAccessPolicyTest {

  private final TestPlanAccessPolicy policy = new TestPlanAccessPolicy();

  /** 저축은행중앙회 계좌통합관리 담당자. 아래 테스트의 대상 기관이 아니다. */
  private static final CurrentUser CHAKA_MEMBER =
      new CurrentUser(
          "yoon.seoyeon@fsb.example.com",
          "윤서연",
          "050",
          "저축은행중앙회",
          OrganizationType.FINANCE_ORGANIZATION,
          false,
          Set.of(Business.ACCOUNT_INFO),
          JobRole.BUSINESS,
          Set.of());

  /** 하하통신(이용기관) 담당자. */
  private static final CurrentUser HAHA_MEMBER =
      new CurrentUser(
          "oh.eunbi@haha.example.com",
          "오은비",
          "HAHA",
          "하하통신",
          OrganizationType.USE_ORGANIZATION,
          false,
          Set.of(Business.ACCOUNT_INFO),
          JobRole.BUSINESS,
          Set.of());

  /** KB국민은행·삼성증권이 대상인 계좌통합관리 기관 테스트. */
  private static TestPlan plan() {
    TestPlan plan =
        TestPlan.create(
            Business.ACCOUNT_INFO,
            new TestPlan.Content(
                "계좌정보 조회·해지", "전문 개선", null, LocalDate.now(), LocalDate.now().plusDays(7)),
            "hong.sungmin@kftc.example.com",
            "홍성민");
    plan.addTarget(org("004", "KB국민은행"));
    plan.addTarget(org("240", "삼성증권"));
    return plan;
  }

  private static Organization org(String code, String name) {
    return new Organization(code, name, OrganizationType.FINANCE_ORGANIZATION, null);
  }

  private static TestPlanTarget target(TestPlan plan, String code) {
    return plan.findTargetByCode(code).orElseThrow();
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
    assertThatThrownBy(() -> policy.ensureCanManage(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 결제원_담당자와_대상_기관_담당자는_테스트를_본다() {
    TestPlan plan = plan();
    assertThat(policy.canView(TestUsers.KFTC_ACCOUNT_INFO, plan)).isTrue();
    assertThat(policy.canView(TestUsers.GANA_MEMBER, plan)).isTrue();
  }

  @Test
  void 대상이_아닌_기관과_다른_업무_결제원과_이용기관은_보지_못한다() {
    TestPlan plan = plan();
    assertThat(policy.canView(CHAKA_MEMBER, plan)).isFalse();
    assertThat(policy.canView(TestUsers.KFTC_CMS, plan)).isFalse();
    assertThat(policy.canView(HAHA_MEMBER, plan)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanView(CHAKA_MEMBER, plan))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 결제원_담당자는_모든_기관_결과를_기관_담당자는_자기_기관_결과를_본다() {
    TestPlan plan = plan();
    assertThat(policy.canViewTarget(TestUsers.KFTC_ACCOUNT_INFO, target(plan, "240"))).isTrue();
    assertThat(policy.canViewTarget(TestUsers.GANA_MEMBER, target(plan, "004"))).isTrue();
  }

  @Test
  void 같은_테스트의_다른_대상_기관_결과는_보지_못한다() {
    TestPlan plan = plan();
    assertThat(policy.canViewTarget(TestUsers.GANA_MEMBER, target(plan, "240"))).isFalse();
    assertThatThrownBy(() -> policy.ensureCanViewTarget(TestUsers.SAA_MEMBER, target(plan, "004")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 기관_담당자는_자기_기관_결과를_등록한다() {
    TestPlan plan = plan();
    assertThat(policy.canReport(TestUsers.GANA_MEMBER, target(plan, "004"))).isTrue();
    assertThat(policy.canReport(TestUsers.SAA_MEMBER, target(plan, "240"))).isTrue();
  }

  @Test
  void 다른_기관_결과와_결제원_대리_등록은_하지_못한다() {
    TestPlan plan = plan();
    assertThat(policy.canReport(TestUsers.GANA_MEMBER, target(plan, "240"))).isFalse();
    assertThat(policy.canReport(TestUsers.KFTC_ACCOUNT_INFO, target(plan, "004"))).isFalse();
    assertThat(policy.canReport(TestUsers.ADMIN, target(plan, "004"))).isFalse();
    assertThatThrownBy(
            () -> policy.ensureCanReport(TestUsers.KFTC_ACCOUNT_INFO, target(plan, "004")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 결제원_담당자는_결과에_의견을_남긴다() {
    assertThat(policy.canFeedback(TestUsers.KFTC_ACCOUNT_INFO, plan())).isTrue();
    assertThat(policy.canFeedback(TestUsers.ADMIN, plan())).isTrue();
  }

  @Test
  void 기관_담당자와_다른_업무_결제원은_의견을_남기지_못한다() {
    TestPlan plan = plan();
    assertThat(policy.canFeedback(TestUsers.GANA_MEMBER, plan)).isFalse();
    assertThat(policy.canFeedback(TestUsers.KFTC_CMS, plan)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanFeedback(TestUsers.GANA_MEMBER, plan))
        .isInstanceOf(AccessDeniedException.class);
  }
}
