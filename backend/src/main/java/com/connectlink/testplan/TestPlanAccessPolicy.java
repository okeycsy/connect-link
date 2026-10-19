package com.connectlink.testplan;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 기관 테스트의 권한 규칙.
 *
 * <ul>
 *   <li>결제원 담당자(그 업무를 맡은 결제원 계정과 관리자): 그 업무의 모든 테스트와 전 기관 결과를 보고, 테스트를 만들고 고치고 지우며, 기관 결과에 의견(미니
 *       수행률·특이사항)을 남기고, 알림 대상을 정한다. 운영·개발·분석·사업부서 담당자의 권한은 같아서 직무로 나누지 않는다.
 *   <li>기관 담당자(그 업무를 맡은 참가기관 계정): 자기 기관이 대상인 테스트만 보고, 자기 기관 결과만 보고 등록한다. 다른 기관 결과는 볼 수 없다.
 *   <li>결제원 담당자는 기관 대신 결과를 등록하지 않는다.
 *   <li>이용기관 담당자는 쓰지 않는다(명세가 정하기 전까지, CLAUDE.md 용어집).
 * </ul>
 */
@Component
public class TestPlanAccessPolicy {

  static final String VIEW_DENIED = "이 기관 테스트를 볼 수 없습니다.";
  static final String MANAGE_DENIED = "그 업무의 결제원 담당자만 기관 테스트를 만들고 고칩니다.";
  static final String TARGET_DENIED = "자기 기관의 테스트 결과만 볼 수 있습니다.";
  static final String REPORT_DENIED = "대상 기관의 그 업무 담당자만 자기 기관 결과를 등록합니다.";
  static final String FEEDBACK_DENIED = "그 업무의 결제원 담당자만 결과에 의견을 남깁니다.";

  /** 이 업무의 기관 테스트를 결제원 담당자로서 다루는지(전부 조회, 만들기·고치기·지우기, 의견, 알림 설정). */
  public boolean canManage(CurrentUser user, Business business) {
    return user.admin()
        || (user.organizationType() == OrganizationType.KFTC
            && user.businesses().contains(business));
  }

  /** 이 업무에서 기관 담당자로서 자기 기관 테스트를 다루는지. 참가기관의 그 업무 담당자다. */
  public boolean isOrganizationMember(CurrentUser user, Business business) {
    return !user.admin()
        && user.organizationType() == OrganizationType.FINANCE_ORGANIZATION
        && user.businesses().contains(business);
  }

  /** 자기 기관이 이 테스트의 대상인 기관 담당자인지. */
  public boolean isTargetMember(CurrentUser user, TestPlan plan) {
    return isOrganizationMember(user, plan.getBusiness())
        && plan.findTargetByCode(user.organizationCode()).isPresent();
  }

  public boolean canView(CurrentUser user, TestPlan plan) {
    return canManage(user, plan.getBusiness()) || isTargetMember(user, plan);
  }

  /** 대상 기관 한 곳의 결과를 볼 수 있는지. 결제원 담당자는 전 기관, 기관 담당자는 자기 기관만. */
  public boolean canViewTarget(CurrentUser user, TestPlanTarget target) {
    TestPlan plan = target.getPlan();
    return canManage(user, plan.getBusiness())
        || (isOrganizationMember(user, plan.getBusiness())
            && target.getOrganization().getCode().equals(user.organizationCode()));
  }

  /** 대상 기관 한 곳의 결과를 등록할 수 있는지. 그 기관의 그 업무 담당자만. */
  public boolean canReport(CurrentUser user, TestPlanTarget target) {
    TestPlan plan = target.getPlan();
    return isOrganizationMember(user, plan.getBusiness())
        && target.getOrganization().getCode().equals(user.organizationCode());
  }

  public boolean canFeedback(CurrentUser user, TestPlan plan) {
    return canManage(user, plan.getBusiness());
  }

  public void ensureCanManage(CurrentUser user, Business business) {
    if (!canManage(user, business)) {
      throw new AccessDeniedException(MANAGE_DENIED);
    }
  }

  public void ensureCanView(CurrentUser user, TestPlan plan) {
    if (!canView(user, plan)) {
      throw new AccessDeniedException(VIEW_DENIED);
    }
  }

  public void ensureCanViewTarget(CurrentUser user, TestPlanTarget target) {
    if (!canViewTarget(user, target)) {
      throw new AccessDeniedException(TARGET_DENIED);
    }
  }

  public void ensureCanReport(CurrentUser user, TestPlanTarget target) {
    if (!canReport(user, target)) {
      throw new AccessDeniedException(REPORT_DENIED);
    }
  }

  public void ensureCanFeedback(CurrentUser user, TestPlan plan) {
    if (!canFeedback(user, plan)) {
      throw new AccessDeniedException(FEEDBACK_DENIED);
    }
  }
}
