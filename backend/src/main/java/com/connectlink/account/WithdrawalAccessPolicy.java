package com.connectlink.account;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.OrganizationType;
import java.util.Collections;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 다른 사람의 계정을 탈퇴 처리하는 권한 규칙. 참가기관 담당자의 계정만 대상이고, 그 담당자의 업무 중 하나라도 맡은 결제원 담당자나 관리자가 처리한다(가입 승인과 같은
 * 사람들이다). 결제원 담당자·관리자 계정은 본인만 탈퇴한다.
 */
@Component
public class WithdrawalAccessPolicy {

  static final String DENIED_MESSAGE = "탈퇴 처리는 그 담당자의 업무를 맡은 결제원 담당자나 관리자가 참가기관 담당자에게만 합니다.";

  public boolean canWithdraw(CurrentUser actor, UserAccount target) {
    if (target.getOrganization().getType() != OrganizationType.FINANCE_ORGANIZATION) {
      return false;
    }
    return actor.admin()
        || (actor.organizationType() == OrganizationType.KFTC
            && !Collections.disjoint(actor.businesses(), target.getBusinesses()));
  }

  public void ensureCanWithdraw(CurrentUser actor, UserAccount target) {
    if (!canWithdraw(actor, target)) {
      throw new AccessDeniedException(DENIED_MESSAGE);
    }
  }
}
