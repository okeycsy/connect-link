package com.connectlink.chat;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * 소통 채널의 권한 규칙.
 *
 * <ul>
 *   <li>방은 구성원만 보고 메시지를 보낸다. 업무 단체방 구성원은 그 업무를 맡은 결제원·참가기관 계정(관리자 포함)과 따로 초대한 사람이다({@link
 *       ChatMembership}).
 *   <li>방을 만들고 사람을 초대하는 것은 결제원 담당자(그 업무를 맡은 결제원 계정과 관리자)만 한다(명세 "결제원 담당자만 채팅방 초대가 가능하다").
 *   <li>따로 초대받은 사람은 나갈 수 있고, 업무 단체방의 업무 담당자는 나가지 못한다(업무가 바뀌면 저절로 빠진다).
 *   <li>확인 공감은 다른 사람이 보낸 메시지에만 남긴다.
 *   <li>이용기관 담당자는 쓰지 않는다(명세가 정하기 전까지, CLAUDE.md 용어집).
 * </ul>
 */
@Component
public class ChatAccessPolicy {

  static final String VIEW_DENIED = "이 채팅방의 구성원이 아닙니다.";
  static final String MANAGE_DENIED = "채팅방을 만들고 초대하는 것은 그 업무의 결제원 담당자만 합니다.";

  /** 이 업무에서 방을 만들고 초대하는지(결제원 담당자). */
  public boolean canManage(CurrentUser user, Business business) {
    return user.admin()
        || (user.organizationType() == OrganizationType.KFTC
            && user.businesses().contains(business));
  }

  public boolean canInvite(CurrentUser user, ChatRoom room, boolean member) {
    return member && canManage(user, room.getBusiness());
  }

  /** 나갈 수 있는지. 따로 초대받은 사람만 나간다. */
  public boolean canLeave(CurrentUser user, ChatRoom room, boolean member) {
    return member && room.findMember(user.email()).isPresent();
  }

  public boolean canAck(CurrentUser user, ChatMessage message, boolean member) {
    return member && !message.getSenderEmail().equals(user.email());
  }

  /** 첨부를 올릴 수 있는지. 첨부를 올린다고 하고 보낸 본인이 구성원일 때, 보관 기간 전에만. */
  public boolean canAttach(CurrentUser user, ChatMessage message, boolean member) {
    return member
        && message.getSenderEmail().equals(user.email())
        && message.isWithAttachments()
        && !message.isAttachmentsExpired();
  }

  void ensureCanManage(CurrentUser user, Business business) {
    if (!canManage(user, business)) {
      throw new AccessDeniedException(MANAGE_DENIED);
    }
  }

  void ensureMember(boolean member) {
    if (!member) {
      throw new AccessDeniedException(VIEW_DENIED);
    }
  }
}
