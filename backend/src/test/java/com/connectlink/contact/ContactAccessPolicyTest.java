package com.connectlink.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.connectlink.auth.CurrentUser;
import com.connectlink.auth.TestUsers;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** 담당자 연락처 권한 규칙마다 허용과 거부를 하나씩 본다. */
class ContactAccessPolicyTest {

  private final ContactAccessPolicy policy = new ContactAccessPolicy();

  private static final Organization KFTC =
      new Organization("KFTC", "금융결제원", OrganizationType.KFTC, null);
  private static final Organization KB =
      new Organization("004", "KB국민은행", OrganizationType.FINANCE_ORGANIZATION, null);
  private static final Organization SHINHAN =
      new Organization("088", "신한은행", OrganizationType.FINANCE_ORGANIZATION, null);

  /** 결제원 상황실. 맡은 업무는 CMS 뿐이다. */
  private static final CurrentUser CONTROL_ROOM =
      new CurrentUser(
          "lee.control@kftc.example.com",
          "이상황",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          false,
          Set.of(Business.CMS),
          JobRole.IT,
          Set.of(ItRole.CONTROL_ROOM));

  /** 결제원 대외계. 맡은 업무는 CMS 뿐이다. */
  private static final CurrentUser EXTERNAL_LINK =
      new CurrentUser(
          "ext@kftc.example.com",
          "강대외",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          false,
          Set.of(Business.CMS),
          JobRole.IT,
          Set.of(ItRole.EXTERNAL_LINK));

  /** 결제원 개발. 맡은 업무는 CMS 뿐이다(전 업무 조회 직무가 아니다). */
  private static final CurrentUser KFTC_DEV =
      new CurrentUser(
          "dev@kftc.example.com",
          "오개발",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          false,
          Set.of(Business.CMS),
          JobRole.IT,
          Set.of(ItRole.DEV));

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

  private static Contact contact(Organization organization, Business business, String email) {
    return Contact.register(
        organization,
        business,
        new Contact.Content(
            null,
            "담당자",
            "부서",
            null,
            null,
            JobRole.BUSINESS,
            Set.of(),
            ContactRank.PRIMARY,
            "1000",
            null,
            email),
        "seed",
        LocalDateTime.now());
  }

  private static final Contact KB_ACCOUNT_INFO =
      contact(KB, Business.ACCOUNT_INFO, "kim.gana@kb.example.com");
  private static final Contact KB_COLLEAGUE =
      contact(KB, Business.ACCOUNT_INFO, "namgung.min@kb.example.com");
  private static final Contact KB_CMS = contact(KB, Business.CMS, "kook.haesol@kb.example.com");
  private static final Contact SHINHAN_ACCOUNT_INFO =
      contact(SHINHAN, Business.ACCOUNT_INFO, "park.dara@shinhan.example.com");
  private static final Contact KFTC_ACCOUNT_INFO =
      contact(KFTC, Business.ACCOUNT_INFO, "hong.gildong@kftc.example.com");

  @Test
  void 결제원_담당자는_맡은_업무의_모든_기관_담당자를_본다() {
    assertThat(policy.canView(TestUsers.KFTC_ACCOUNT_INFO, SHINHAN_ACCOUNT_INFO)).isTrue();
    assertThat(policy.canView(TestUsers.KFTC_ACCOUNT_INFO, KB_ACCOUNT_INFO)).isTrue();
  }

  @Test
  void 결제원_담당자도_맡지_않은_업무의_담당자는_보지_못한다() {
    assertThat(policy.canView(TestUsers.KFTC_ACCOUNT_INFO, KB_CMS)).isFalse();
    assertThat(policy.canList(TestUsers.KFTC_CMS, Business.ACCOUNT_INFO)).isFalse();
  }

  @Test
  void 결제원_운영_상황실_대외계는_맡지_않은_업무까지_전_기관을_본다() {
    assertThat(policy.canView(CONTROL_ROOM, KB_ACCOUNT_INFO)).isTrue();
    assertThat(policy.canView(EXTERNAL_LINK, SHINHAN_ACCOUNT_INFO)).isTrue();
    assertThat(policy.canList(CONTROL_ROOM, Business.LOAN_TRANSFER)).isTrue();
  }

  @Test
  void 결제원_개발은_맡지_않은_업무를_보지_못한다() {
    assertThat(policy.canView(KFTC_DEV, KB_ACCOUNT_INFO)).isFalse();
  }

  @Test
  void 결제원_운영_상황실도_맡지_않은_업무는_고치지_못한다() {
    assertThat(policy.canEdit(CONTROL_ROOM, KB_ACCOUNT_INFO)).isFalse();
    assertThat(policy.canDelete(CONTROL_ROOM, KB_ACCOUNT_INFO)).isFalse();
  }

  @Test
  void 기관_담당자는_결제원_담당자와_자기_기관_같은_업무_담당자를_본다() {
    assertThat(policy.canView(TestUsers.GANA_MEMBER, KFTC_ACCOUNT_INFO)).isTrue();
    assertThat(policy.canView(TestUsers.GANA_MEMBER, KB_COLLEAGUE)).isTrue();
  }

  @Test
  void 기관_담당자는_다른_기관이나_맡지_않은_업무_담당자를_보지_못한다() {
    assertThat(policy.canView(TestUsers.GANA_MEMBER, SHINHAN_ACCOUNT_INFO)).isFalse();
    assertThat(policy.canView(TestUsers.GANA_MEMBER, KB_CMS)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanView(TestUsers.GANA_MEMBER, SHINHAN_ACCOUNT_INFO))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 기관_담당자는_본인과_자기_기관_같은_업무_담당자를_고친다() {
    assertThat(policy.canEdit(TestUsers.GANA_MEMBER, KB_ACCOUNT_INFO)).isTrue();
    assertThat(policy.canEdit(TestUsers.GANA_MEMBER, KB_COLLEAGUE)).isTrue();
  }

  @Test
  void 기관_담당자는_결제원_담당자나_다른_기관_담당자를_고치지_못한다() {
    assertThat(policy.canEdit(TestUsers.GANA_MEMBER, KFTC_ACCOUNT_INFO)).isFalse();
    assertThat(policy.canEdit(TestUsers.GANA_MEMBER, SHINHAN_ACCOUNT_INFO)).isFalse();
  }

  @Test
  void 결제원_담당자는_맡은_업무의_담당자를_고치고_지운다() {
    assertThat(policy.canEdit(TestUsers.KFTC_ACCOUNT_INFO, SHINHAN_ACCOUNT_INFO)).isTrue();
    assertThat(policy.canDelete(TestUsers.KFTC_ACCOUNT_INFO, SHINHAN_ACCOUNT_INFO)).isTrue();
  }

  @Test
  void 기관_담당자는_지우지_못한다() {
    assertThat(policy.canDelete(TestUsers.GANA_MEMBER, KB_COLLEAGUE)).isFalse();
    assertThatThrownBy(() -> policy.ensureCanDelete(TestUsers.GANA_MEMBER, KB_COLLEAGUE))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void 기관_담당자는_자기_기관에만_후임자를_등록한다() {
    assertThat(policy.canCreate(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO, KB)).isTrue();
    assertThat(policy.canCreate(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO, SHINHAN)).isFalse();
    assertThat(policy.canCreate(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO, KFTC)).isFalse();
  }

  @Test
  void 정기_변경_주간은_그_업무_결제원_담당자만_다룬다() {
    assertThat(policy.canManage(TestUsers.KFTC_ACCOUNT_INFO, Business.ACCOUNT_INFO)).isTrue();
    assertThat(policy.canManage(TestUsers.GANA_MEMBER, Business.ACCOUNT_INFO)).isFalse();
    assertThat(policy.canManage(CONTROL_ROOM, Business.ACCOUNT_INFO)).isFalse();
  }

  @Test
  void 관리자는_전_업무를_다룬다() {
    assertThat(policy.canManage(TestUsers.ADMIN, Business.LOAN_TRANSFER)).isTrue();
    assertThat(policy.canDelete(TestUsers.ADMIN, KB_CMS)).isTrue();
  }

  @Test
  void 이용기관_담당자는_쓰지_않는다() {
    assertThat(policy.canList(HAHA_MEMBER, Business.ACCOUNT_INFO)).isFalse();
    assertThat(policy.canView(HAHA_MEMBER, KFTC_ACCOUNT_INFO)).isFalse();
  }

  @Test
  void 결제원_쪽_변경인지는_기관_구분으로_가린다() {
    assertThat(policy.isKftcSide(TestUsers.KFTC_CMS)).isTrue();
    assertThat(policy.isKftcSide(TestUsers.GANA_MEMBER)).isFalse();
  }
}
