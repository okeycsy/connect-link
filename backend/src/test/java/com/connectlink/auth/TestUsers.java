package com.connectlink.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * MockMvc 요청에 로그인 사용자를 싣는다. 권한 테스트는 허용 사용자와 거부 사용자를 하나씩 쓴다. 값은 계정 시드(R__2_account_seed.sql)와 같다.
 */
public final class TestUsers {

  /** 결제원 데모 관리자(전 업무, 전산 운영, 관리자). */
  public static final CurrentUser ADMIN =
      new CurrentUser(
          "admin@kftc.example.com",
          "데모 관리자",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          true,
          EnumSet.allOf(Business.class),
          JobRole.IT,
          Set.of(ItRole.OPS));

  /** 가나은행 계좌통합관리 현업 담당자(관리자 아님). 알림 시드에 이 사람 앞으로 1건이 있다. */
  public static final CurrentUser GANA_MEMBER =
      new CurrentUser(
          "kim.gana@gana.example.com",
          "김가나",
          "004",
          "가나은행",
          OrganizationType.FINANCE_ORGANIZATION,
          false,
          Set.of(Business.ACCOUNT_INFO),
          JobRole.BUSINESS,
          Set.of());

  /** 사아증권 계좌통합관리 현업 담당자(관리자 아님). 의견 요청에서 가나은행과 다른 대상 기관 담당자로 쓴다. */
  public static final CurrentUser SAA_MEMBER =
      new CurrentUser(
          "choi.saa@saa.example.com",
          "최사아",
          "240",
          "사아증권",
          OrganizationType.FINANCE_ORGANIZATION,
          false,
          Set.of(Business.ACCOUNT_INFO),
          JobRole.BUSINESS,
          Set.of());

  /** 결제원 계좌통합관리·자동이체통합관리 현업 담당자(관리자 아님). 그 업무의 가입 신청을 승인한다. */
  public static final CurrentUser KFTC_ACCOUNT_INFO =
      new CurrentUser(
          "hong.gildong@kftc.example.com",
          "홍길동",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          false,
          Set.of(Business.ACCOUNT_INFO, Business.AUTO_TRANSFER),
          JobRole.BUSINESS,
          Set.of());

  /** 결제원 CMS 현업 담당자(관리자 아님). */
  public static final CurrentUser KFTC_CMS =
      new CurrentUser(
          "park.cms@kftc.example.com",
          "박하늘",
          "KFTC",
          "금융결제원",
          OrganizationType.KFTC,
          false,
          Set.of(Business.CMS),
          JobRole.BUSINESS,
          Set.of());

  private TestUsers() {}

  public static RequestPostProcessor admin() {
    return as(ADMIN);
  }

  public static RequestPostProcessor ganaMember() {
    return as(GANA_MEMBER);
  }

  public static RequestPostProcessor as(CurrentUser user) {
    return authentication(CurrentUserAuthentication.of(user));
  }
}
