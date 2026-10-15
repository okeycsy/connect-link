package com.connectlink.organization;

import com.connectlink.auth.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** 기준정보 모듈의 권한 규칙. 기관·업무 기준정보의 상세 조회와 수정은 관리자만 한다. 이름·구분만 있는 공개 목록은 누구나 본다. */
@Component
public class OrganizationAccessPolicy {

  public boolean canManage(CurrentUser user) {
    return user.admin();
  }

  public void ensureCanManage(CurrentUser user) {
    if (!canManage(user)) {
      throw new AccessDeniedException("기관·업무 기준정보는 관리자만 다룰 수 있습니다.");
    }
  }
}
