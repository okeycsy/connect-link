package com.connectlink.contact;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.ItRole;
import com.connectlink.organization.Organization;
import com.connectlink.organization.OrganizationType;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 담당자 연락처의 권한 규칙(명세 "가. 참가기관 담당자 연락처 관리").
 *
 * <ul>
 *   <li>결제원 담당자(그 업무를 맡은 결제원 계정과 관리자): 그 업무의 모든 기관 담당자를 보고, 등록·수정·삭제한다. 등록·수정·삭제·확인은 내부망에서만 한다(망
 *       판단은 Service 가 {@code NetworkZoneResolver} 로 한다).
 *   <li>결제원 운영·상황실·대외계: 맡은 업무와 상관없이 전 업무·전 기관 담당자를 본다.
 *   <li>기관 담당자(그 업무를 맡은 참가기관 계정): 그 업무의 결제원 담당자와 자기 기관 담당자만 본다. 본인과 자기 기관의 같은 업무 담당자를 고치고 확인하며,
 *       후임자를 등록한다. 지우지는 않는다.
 *   <li>이용기관 담당자는 쓰지 않는다(명세가 정하기 전까지, CLAUDE.md 용어집).
 * </ul>
 */
@Component
public class ContactAccessPolicy {

  static final String VIEW_DENIED = "이 담당자 정보를 볼 수 없습니다.";
  static final String EDIT_DENIED = "본인과 소속 기관의 같은 업무 담당자, 또는 그 업무의 결제원 담당자만 고칩니다.";
  static final String CREATE_DENIED = "그 업무의 결제원 담당자나 그 기관의 같은 업무 담당자만 담당자를 등록합니다.";
  static final String DELETE_DENIED = "그 업무의 결제원 담당자만 담당자를 지웁니다.";
  static final String CAMPAIGN_DENIED = "그 업무의 결제원 담당자만 정기 변경 주간을 등록하고 진행률을 봅니다.";

  /** 맡은 업무와 상관없이 전 업무·전 기관 담당자를 조회하는 결제원 전산 세부 직무(명세). */
  private static final Set<ItRole> ALL_BUSINESS_ROLES =
      EnumSet.of(ItRole.OPS, ItRole.CONTROL_ROOM, ItRole.EXTERNAL_LINK);

  /** 이 업무에서 결제원 담당자로서 연락처를 관리하는지(전 기관 조회·등록·수정·삭제, 정기 변경 주간). */
  public boolean canManage(CurrentUser user, Business business) {
    return user.admin()
        || (user.organizationType() == OrganizationType.KFTC
            && user.businesses().contains(business));
  }

  /** 이 업무의 전 기관 담당자를 보는지. 관리하는 결제원 담당자와 결제원 운영·상황실·대외계다. */
  public boolean canViewAll(CurrentUser user, Business business) {
    return canManage(user, business)
        || (user.organizationType() == OrganizationType.KFTC
            && user.itRoles().stream().anyMatch(ALL_BUSINESS_ROLES::contains));
  }

  /** 이 업무에서 기관 담당자로서 자기 기관 담당자를 다루는지. 그 업무를 맡은 참가기관 계정이다. */
  public boolean isOrganizationMember(CurrentUser user, Business business) {
    return !user.admin()
        && user.organizationType() == OrganizationType.FINANCE_ORGANIZATION
        && user.businesses().contains(business);
  }

  /** 이 업무의 담당자 목록을 볼 수 있는지. */
  public boolean canList(CurrentUser user, Business business) {
    return canViewAll(user, business) || isOrganizationMember(user, business);
  }

  public boolean canView(CurrentUser user, Contact contact) {
    Business business = contact.getBusiness();
    if (canViewAll(user, business)) {
      return true;
    }
    return isOrganizationMember(user, business)
        && (contact.getOrganization().getType() == OrganizationType.KFTC
            || isOwnOrganization(user, contact.getOrganization()));
  }

  /** 고치고 "변동 없음" 으로 확인할 수 있는지. 결제원 담당자는 그 업무 전부, 기관 담당자는 본인과 자기 기관의 같은 업무 담당자다. */
  public boolean canEdit(CurrentUser user, Contact contact) {
    return canManage(user, contact.getBusiness())
        || (isOrganizationMember(user, contact.getBusiness())
            && isOwnOrganization(user, contact.getOrganization()));
  }

  public boolean canDelete(CurrentUser user, Contact contact) {
    return canManage(user, contact.getBusiness());
  }

  /** 이 기관의 이 업무 담당자를 등록할 수 있는지. 기관 담당자는 자기 기관에 후임자 등을 등록한다. */
  public boolean canCreate(CurrentUser user, Business business, Organization organization) {
    return canManage(user, business)
        || (isOrganizationMember(user, business) && isOwnOrganization(user, organization));
  }

  /** 결제원 쪽 변경인지. 결제원 담당자의 수정·삭제는 내부망에서만 한다(명세). 기관 담당자는 인터넷망에서도 고친다. */
  public boolean isKftcSide(CurrentUser user) {
    return user.admin() || user.organizationType() == OrganizationType.KFTC;
  }

  private static boolean isOwnOrganization(CurrentUser user, Organization organization) {
    return organization.getCode().equals(user.organizationCode());
  }

  public void ensureCanList(CurrentUser user, Business business) {
    if (!canList(user, business)) {
      throw new AccessDeniedException(VIEW_DENIED);
    }
  }

  public void ensureCanView(CurrentUser user, Contact contact) {
    if (!canView(user, contact)) {
      throw new AccessDeniedException(VIEW_DENIED);
    }
  }

  public void ensureCanEdit(CurrentUser user, Contact contact) {
    if (!canEdit(user, contact)) {
      throw new AccessDeniedException(EDIT_DENIED);
    }
  }

  public void ensureCanDelete(CurrentUser user, Contact contact) {
    if (!canDelete(user, contact)) {
      throw new AccessDeniedException(DELETE_DENIED);
    }
  }

  public void ensureCanCreate(CurrentUser user, Business business, Organization organization) {
    if (!canCreate(user, business, organization)) {
      throw new AccessDeniedException(CREATE_DENIED);
    }
  }

  public void ensureCanManage(CurrentUser user, Business business) {
    if (!canManage(user, business)) {
      throw new AccessDeniedException(CAMPAIGN_DENIED);
    }
  }
}
