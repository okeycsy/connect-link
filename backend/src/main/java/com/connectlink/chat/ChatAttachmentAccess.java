package com.connectlink.chat;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅 메시지 첨부 권한. 소통 채널에서는 파일을 올리지 못한다(2026-11-15, 결제원 파일 반출은 팀장 결재가 필요해 대화방 파일 전송을 없앴다). 그 전에 보낸 파일은
 * 방 구성원이 보관 기간까지 내려받는다.
 */
@Component
@Transactional(readOnly = true)
class ChatAttachmentAccess implements AttachmentOwnerAccess {

  private final ChatMessageRepository messageRepository;
  private final ChatMembership membership;

  ChatAttachmentAccess(ChatMessageRepository messageRepository, ChatMembership membership) {
    this.messageRepository = messageRepository;
    this.membership = membership;
  }

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.CHAT_MESSAGE;
  }

  @Override
  public boolean canView(CurrentUser user, Long ownerId) {
    return messageRepository
        .findById(ownerId)
        .map(m -> membership.isMember(user, m.getRoom()))
        .orElse(false);
  }

  /** 아무도 올리거나 지우지 못한다. */
  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return false;
  }
}
