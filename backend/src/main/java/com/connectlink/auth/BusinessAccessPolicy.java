package com.connectlink.auth;

import com.connectlink.organization.Business;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 업무 영역(레일)의 조회 범위. 관리자는 전 업무, 그 밖의 사용자는 계정의 담당 업무만 본다(결제원 운영·상황실은 계정에 전 업무가 들어 있다). 업무 영역을 받는 공통
 * API(홈, 검색)가 이 규칙을 쓰고, 기능 모듈의 {@code XxxAccessPolicy} 는 이 범위 안에서 더 좁힌다.
 */
@Component
public class BusinessAccessPolicy {

  public boolean canView(CurrentUser user, Business business) {
    return user.admin() || user.businesses().contains(business);
  }

  public void ensureCanView(CurrentUser user, Business business) {
    if (!canView(user, business)) {
      throw new AccessDeniedException(business.getLabel() + " 은 담당 업무가 아니어서 볼 수 없습니다.");
    }
  }
}
