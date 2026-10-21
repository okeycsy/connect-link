package com.connectlink.suspension;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 일시중단의 권한 규칙.
 *
 * <ul>
 *   <li>결제원 담당자(그 업무를 맡은 결제원 계정과 관리자): 그 업무의 모든 기관 일시중단을 보고 등록하고, 기관이 올린 것도 고친다. 확인을 시작하고 조치·인수인계
 *       사항을 써서 확정한다.
 *   <li>기관 담당자(그 업무를 맡은 참가기관 계정): 자기 기관의 그 업무 일시중단을 본다. 본인이 올린 것만 고치고 지운다.
 *   <li>등록은 사람이 아니라 기관 기준이다. 참가기관 계정은 자기 기관이 참여하는 업무 어디에나, 결제원 계정은 고른 기관이 참여하는 업무 어디에나 올린다(대외계·네트워크
 *       작업은 한 업무에 국한되지 않는다). 등록한 사람은 맡지 않은 업무의 건이라도 자기가 올린 건을 보고 고친다.
 *   <li>지우기는 등록한 본인만 한다(결제원 담당자도 자기가 올린 것만). 완료한 건은 아무도 고치거나 지우지 않는다.
 *   <li>이용기관 담당자는 쓰지 않는다(명세가 정하기 전까지, CLAUDE.md 용어집).
 * </ul>
 */
@Component
public class SuspensionAccessPolicy {

  static final String VIEW_DENIED = "이 일시중단을 볼 수 없습니다.";
  static final String REGISTER_DENIED = "결제원 담당자나 그 기관의 참가기관 담당자만 일시중단을 등록합니다.";
  static final String EDIT_DENIED = "일시중단은 등록한 기관 담당자나 그 업무의 결제원 담당자만 고칩니다.";
  static final String DELETE_DENIED = "일시중단은 등록한 본인만 지웁니다.";
  static final String REVIEW_DENIED = "그 업무의 결제원 담당자만 확인하고 확정합니다.";
  static final String DIGEST_DENIED = "전 영업일 취합은 결제원 담당자만 보냅니다.";

  /** 이 업무의 일시중단을 결제원 담당자로서 다루는지(전 기관 조회, 수정, 확인·확정). */
  public boolean canManage(CurrentUser user, Business business) {
    return user.admin()
        || (user.organizationType() == OrganizationType.KFTC
            && user.businesses().contains(business));
  }

  /** 이 업무에서 기관 담당자로서 자기 기관 일시중단을 다루는지. 참가기관의 그 업무 담당자다. */
  public boolean isOrganizationMember(CurrentUser user, Business business) {
    return !user.admin()
        && user.organizationType() == OrganizationType.FINANCE_ORGANIZATION
        && user.businesses().contains(business);
  }

  /** 결제원 쪽(관리자, 결제원 계정)으로 등록하는지. 기관을 골라 올리고 등록자가 결제원으로 남는다. */
  public boolean registersAsKftc(CurrentUser user) {
    return user.admin() || user.organizationType() == OrganizationType.KFTC;
  }

  /** 일시중단을 올릴 수 있는 계정인지. 결제원 계정과 참가기관 계정이다. 업무는 기관이 참여하는지로 따로 본다. */
  public boolean canRegister(CurrentUser user) {
    return registersAsKftc(user)
        || user.organizationType() == OrganizationType.FINANCE_ORGANIZATION;
  }

  /** 이 기관의 이 업무로 올릴 수 있는지. 기관이 그 업무에 참여해야 하고, 참가기관 계정은 자기 기관만이다. */
  public boolean canRegister(CurrentUser user, Organization organization, Business business) {
    if (!organization.participatesIn(business)) {
      return false;
    }
    return registersAsKftc(user)
        || (user.organizationType() == OrganizationType.FINANCE_ORGANIZATION
            && organization.getCode().equals(user.organizationCode()));
  }

  public boolean canView(CurrentUser user, Suspension suspension) {
    return canManage(user, suspension.getBusiness())
        || isOwnOrganization(user, suspension)
        || isActiveRegistrant(user, suspension);
  }

  /** 고칠 수 있는지. 결제원 담당자는 기관이 올린 것도, 기관 담당자는 본인이 올린 것만. */
  public boolean canEdit(CurrentUser user, Suspension suspension) {
    if (suspension.getStatus() == SuspensionStatus.COMPLETED) {
      return false;
    }
    return canManage(user, suspension.getBusiness()) || isActiveRegistrant(user, suspension);
  }

  /** 지울 수 있는지. 등록한 본인이 아직 같은 쪽(결제원, 그 기관)에 있을 때만. */
  public boolean canDelete(CurrentUser user, Suspension suspension) {
    return suspension.getStatus() != SuspensionStatus.COMPLETED
        && isRegistrant(user, suspension)
        && canView(user, suspension);
  }

  /** 확인을 시작하거나 조치·인수인계 사항을 써서 확정할 수 있는지. */
  public boolean canReview(CurrentUser user, Suspension suspension) {
    return suspension.getStatus() != SuspensionStatus.COMPLETED
        && canManage(user, suspension.getBusiness());
  }

  /** 전 영업일 취합을 수동으로 보낼 수 있는지. 취합은 전 업무를 묶으므로 결제원 담당자 누구나와 관리자다. */
  public boolean canSendDigest(CurrentUser user) {
    return user.admin() || user.organizationType() == OrganizationType.KFTC;
  }

  private boolean isOwnOrganization(CurrentUser user, Suspension suspension) {
    return isOrganizationMember(user, suspension.getBusiness())
        && suspension.getOrganization().getCode().equals(user.organizationCode());
  }

  private static boolean isRegistrant(CurrentUser user, Suspension suspension) {
    return suspension.getRegistrantEmail().equals(user.email());
  }

  /** 등록한 본인이고 아직 같은 쪽(결제원이면 결제원, 기관이면 그 기관)에 있는지. 맡은 업무와는 상관없다. */
  private boolean isActiveRegistrant(CurrentUser user, Suspension suspension) {
    if (!isRegistrant(user, suspension)) {
      return false;
    }
    if (suspension.isRegistrantKftc()) {
      return registersAsKftc(user);
    }
    return user.organizationType() == OrganizationType.FINANCE_ORGANIZATION
        && suspension.getOrganization().getCode().equals(user.organizationCode());
  }

  public void ensureCanRegister(CurrentUser user) {
    if (!canRegister(user)) {
      throw new AccessDeniedException(REGISTER_DENIED);
    }
  }

  public void ensureCanRegister(CurrentUser user, Organization organization, Business business) {
    if (!canRegister(user, organization, business)) {
      throw new AccessDeniedException(REGISTER_DENIED);
    }
  }

  public void ensureCanView(CurrentUser user, Suspension suspension) {
    if (!canView(user, suspension)) {
      throw new AccessDeniedException(VIEW_DENIED);
    }
  }

  public void ensureCanEdit(CurrentUser user, Suspension suspension) {
    if (!canEdit(user, suspension)) {
      throw new AccessDeniedException(EDIT_DENIED);
    }
  }

  public void ensureCanDelete(CurrentUser user, Suspension suspension) {
    if (!canDelete(user, suspension)) {
      throw new AccessDeniedException(DELETE_DENIED);
    }
  }

  public void ensureCanReview(CurrentUser user, Suspension suspension) {
    if (!canReview(user, suspension)) {
      throw new AccessDeniedException(REVIEW_DENIED);
    }
  }

  public void ensureCanSendDigest(CurrentUser user) {
    if (!canSendDigest(user)) {
      throw new AccessDeniedException(DIGEST_DENIED);
    }
  }
}
