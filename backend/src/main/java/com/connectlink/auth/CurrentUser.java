package com.connectlink.auth;

import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.JobRole;
import com.connectlink.organization.OrganizationType;
import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

/**
 * 로그인한 사용자. 세션 인증의 principal 이며 Service 의 권한 판단(XxxAccessPolicy)에 넘기는 값이다. 로그인할 때 계정({@code
 * UserAccount})에서 만들어 세션에 둔다. 식별 키는 회사 이메일이다.
 *
 * @param businesses 담당 업무. 레일(업무 영역)의 조회 범위다.
 * @param jobRole 직무(현업/전산)
 * @param itRoles 전산 세부 직무. 현업이면 비어 있다.
 * @param jobTitle 직급(예: 과장). 없으면 null 이다. 글쓴이 표시 "결제원(김민찬 과장)"에 쓴다.
 */
public record CurrentUser(
    String email,
    String name,
    String organizationCode,
    String organizationName,
    OrganizationType organizationType,
    boolean admin,
    Set<Business> businesses,
    JobRole jobRole,
    Set<ItRole> itRoles,
    String jobTitle)
    implements Principal, Serializable {

  public CurrentUser {
    businesses = businesses == null ? Set.of() : Set.copyOf(businesses);
    itRoles = itRoles == null ? Set.of() : Set.copyOf(itRoles);
    jobTitle = jobTitle == null || jobTitle.isBlank() ? null : jobTitle.trim();
  }

  /** 직급 없이 만든다(테스트·권한 판단용). */
  public CurrentUser(
      String email,
      String name,
      String organizationCode,
      String organizationName,
      OrganizationType organizationType,
      boolean admin,
      Set<Business> businesses,
      JobRole jobRole,
      Set<ItRole> itRoles) {
    this(
        email,
        name,
        organizationCode,
        organizationName,
        organizationType,
        admin,
        businesses,
        jobRole,
        itRoles,
        null);
  }

  /** 감사 컬럼(created_by, updated_by)과 로그에 남는 이름. 이메일을 쓴다. */
  @Override
  public String getName() {
    return email;
  }

  /** 화면 표기용 역할. 예: "결제원 · 전산(운영)", "참가기관 · 현업". */
  public String roleLabel() {
    return organizationType.getLabel() + " · " + jobRole.describe(itRoles);
  }
}
