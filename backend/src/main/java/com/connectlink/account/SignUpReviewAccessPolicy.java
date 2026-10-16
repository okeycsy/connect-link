package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 가입 승인의 권한 규칙. 신청한 업무를 맡은 결제원 담당자나 관리자가 승인·반려한다(docs/decisions.md). 업무를 여럿 신청했으면 그중 하나만 맡아도 신청 전체를
 * 처리한다. 참가기관·이용기관 담당자는 가입 신청을 볼 수 없다.
 */
@Component
public class SignUpReviewAccessPolicy {

  static final String DENIED_MESSAGE = "가입 승인은 신청한 업무를 맡은 결제원 담당자나 관리자가 합니다.";

  /** 한 업무 영역의 가입 신청 목록을 볼 수 있는지. */
  public boolean canReview(CurrentUser user, Business business) {
    return user.admin()
        || (user.organizationType() == OrganizationType.KFTC
            && user.businesses().contains(business));
  }

  /** 이 업무들을 신청한 계정을 승인·반려할 수 있는지. */
  public boolean canReview(CurrentUser user, Set<Business> requested) {
    return requested.stream().anyMatch(business -> canReview(user, business));
  }

  public void ensureCanReview(CurrentUser user, Business business) {
    if (!canReview(user, business)) {
      throw new AccessDeniedException(DENIED_MESSAGE);
    }
  }

  public void ensureCanReview(CurrentUser user, Set<Business> requested) {
    if (!canReview(user, requested)) {
      throw new AccessDeniedException(DENIED_MESSAGE);
    }
  }
}
