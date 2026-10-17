package com.connectlink.opinion;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 의견 요청의 권한 규칙.
 *
 * <ul>
 *   <li>요청을 만들고 고치고 회신을 취합하는 사람: 그 업무를 맡은 결제원 담당자와 관리자.
 *   <li>요청을 보고 회신하는 사람: 대상 기관의 같은 업무 담당자(참가기관). 작성중 요청과 다른 기관의 회신은 보지 못한다.
 *   <li>이용기관 담당자는 쓰지 않는다(명세가 정하기 전까지, CLAUDE.md 용어집).
 * </ul>
 */
@Component
public class OpinionAccessPolicy {

  static final String MANAGE_DENIED = "의견 요청은 그 업무를 맡은 결제원 담당자가 만들고 관리합니다.";
  static final String VIEW_DENIED = "이 의견 요청을 볼 수 없습니다.";
  static final String REPLY_DENIED = "의견 요청 대상 기관의 같은 업무 담당자만 회신합니다.";

  /** 이 업무 영역의 의견 요청을 만들고 관리(수정·마감·취합·리마인드)할 수 있는지. */
  public boolean canManage(CurrentUser user, Business business) {
    return user.admin()
        || (user.organizationType() == OrganizationType.KFTC
            && user.businesses().contains(business));
  }

  /** 이 업무 영역에서 기관 담당자로서 회신함을 쓰는지. 참가기관의 그 업무 담당자다. */
  public boolean isReplier(CurrentUser user, Business business) {
    return !user.admin()
        && user.organizationType() == OrganizationType.FINANCE_ORGANIZATION
        && user.businesses().contains(business);
  }

  /** 요청을 볼 수 있는지. 관리하는 사람은 작성중까지, 기관 담당자는 자기 기관이 대상인 열린(회신중·마감) 요청만. */
  public boolean canView(CurrentUser user, OpinionRequest request) {
    return canManage(user, request.getBusiness()) || isTargetMember(user, request);
  }

  /** 이 사용자가 요청의 대상 기관 담당자인지(작성중 요청은 아직 대상에게 보이지 않는다). */
  public boolean isTargetMember(CurrentUser user, OpinionRequest request) {
    return isReplier(user, request.getBusiness())
        && request.getStatus() != OpinionRequestStatus.DRAFT
        && request.getTargets().stream()
            .anyMatch(t -> t.getOrganization().getCode().equals(user.organizationCode()));
  }

  public void ensureCanManage(CurrentUser user, Business business) {
    if (!canManage(user, business)) {
      throw new AccessDeniedException(MANAGE_DENIED);
    }
  }

  public void ensureCanView(CurrentUser user, OpinionRequest request) {
    if (!canView(user, request)) {
      throw new AccessDeniedException(VIEW_DENIED);
    }
  }

  public void ensureTargetMember(CurrentUser user, OpinionRequest request) {
    if (!isTargetMember(user, request)) {
      throw new AccessDeniedException(REPLY_DENIED);
    }
  }
}
