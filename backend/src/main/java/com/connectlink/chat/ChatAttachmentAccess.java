package com.connectlink.chat;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 채팅 메시지 첨부 권한. 방 구성원이면 내려받고, 첨부를 올린다고 하고 보낸 본인만 보관 기간 전에 올리고 지운다. */
@Component
@Transactional(readOnly = true)
class ChatAttachmentAccess implements AttachmentOwnerAccess {

  private final ChatMessageRepository messageRepository;
  private final ChatMembership membership;
  private final ChatAccessPolicy accessPolicy;

  ChatAttachmentAccess(
      ChatMessageRepository messageRepository,
      ChatMembership membership,
      ChatAccessPolicy accessPolicy) {
    this.messageRepository = messageRepository;
    this.membership = membership;
    this.accessPolicy = accessPolicy;
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

  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return messageRepository
        .findById(ownerId)
        .map(m -> accessPolicy.canAttach(user, m, membership.isMember(user, m.getRoom())))
        .orElse(false);
  }
}
