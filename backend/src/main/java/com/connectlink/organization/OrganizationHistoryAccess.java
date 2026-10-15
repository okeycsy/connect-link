package com.connectlink.organization;

import com.connectlink.auth.CurrentUser;
import com.connectlink.history.HistoryTargetAccess;
import com.connectlink.history.HistoryTargetType;
import org.springframework.stereotype.Component;

/** 기관 기준정보의 변경 이력 권한. 기준정보를 관리할 수 있는 사람(관리자)만 본다. */
@Component
public class OrganizationHistoryAccess implements HistoryTargetAccess {

  private final OrganizationAccessPolicy accessPolicy;

  public OrganizationHistoryAccess(OrganizationAccessPolicy accessPolicy) {
    this.accessPolicy = accessPolicy;
  }

  @Override
  public HistoryTargetType targetType() {
    return HistoryTargetType.ORGANIZATION;
  }

  @Override
  public boolean canView(CurrentUser user, Long targetId) {
    return accessPolicy.canManage(user);
  }
}
