package com.connectlink.auth;

import com.connectlink.organization.OrganizationType;
import java.security.Principal;

/**
 * 로그인한 사용자. 세션 인증의 principal 이며 Service 의 권한 판단(XxxAccessPolicy)에 넘기는 값이다.
 *
 * <p>계정 모듈이 붙기 전까지는 {@link DemoUsers} 의 고정 사용자로 채운다. 식별 키는 회사 이메일이다.
 */
public record CurrentUser(
    String email,
    String name,
    String organizationCode,
    String organizationName,
    OrganizationType organizationType,
    boolean admin)
    implements Principal {

  /** 감사 컬럼(created_by, updated_by)과 로그에 남는 이름. 이메일을 쓴다. */
  @Override
  public String getName() {
    return email;
  }
}
