package com.connectlink.notice;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 공지·자료실 권한. 업무 글은 그 업무를 맡은 사람(결제원·참가기관 담당자, 관리자는 전 업무)이 보고 그 업무 결제원 담당자가 쓴다. 전 업무 공통 공지는 모두 보고
 * 관리자가 쓴다. 이용기관 담당자는 명세가 정하기 전까지 쓰지 않는다.
 */
@Component
public class NoticeAccessPolicy {

  static final String VIEW_DENIED = "이 글을 볼 수 없습니다.";
  static final String MANAGE_DENIED = "공지·자료실 글은 그 업무 결제원 담당자가 올리고 고칩니다.";
  static final String COMMON_DENIED = "전 업무 공통 공지는 관리자가 올리고 고칩니다.";

  /** 이 업무 영역의 공지·자료실을 쓰는지. */
  public boolean canList(CurrentUser user, Business business) {
    return user.admin()
        || (user.organizationType() != OrganizationType.USE_ORGANIZATION
            && user.businesses().contains(business));
  }

  public boolean canView(CurrentUser user, Notice notice) {
    if (notice.isCommon()) {
      return user.admin() || user.organizationType() != OrganizationType.USE_ORGANIZATION;
    }
    return canList(user, notice.getBusiness());
  }

  /** 업무 글(business 가 있으면)이나 공통 공지(null)를 올리고 고칠 수 있는지. */
  public boolean canManage(CurrentUser user, Business business) {
    if (business == null) {
      return user.admin();
    }
    return user.admin()
        || (user.organizationType() == OrganizationType.KFTC
            && user.businesses().contains(business));
  }

  public void ensureCanList(CurrentUser user, Business business) {
    if (!canList(user, business)) {
      throw new AccessDeniedException(business.getLabel() + " 은 담당 업무가 아니어서 볼 수 없습니다.");
    }
  }

  public void ensureCanView(CurrentUser user, Notice notice) {
    if (!canView(user, notice)) {
      throw new AccessDeniedException(VIEW_DENIED);
    }
  }

  public void ensureCanManage(CurrentUser user, Business business) {
    if (!canManage(user, business)) {
      throw new AccessDeniedException(business == null ? COMMON_DENIED : MANAGE_DENIED);
    }
  }
}
