package com.connectlink.chat;

import com.connectlink.auth.CurrentUser;
import com.connectlink.home.HomeCardContributor;
import com.connectlink.home.HomeCardKey;
import com.connectlink.home.HomeCardSource;
import com.connectlink.home.HomeContribution;
import com.connectlink.home.Urgency;
import com.connectlink.home.dto.HomeCard;
import com.connectlink.home.dto.HomeCard.HomeCardItem;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 "우리 기관을 부른 대화" 카드. 참가기관 담당자에게 그 업무의 채팅방에서 자기 기관을 멘션했는데 아직 읽지 않은 메시지를 최근 것부터 보인다. 결제원과 이용기관에는
 * 카드가 없다.
 */
@Component
@Transactional(readOnly = true)
public class ChatMentionHomeCard implements HomeCardContributor {

  private static final int ITEM_COUNT = 3;
  private static final int PREVIEW_LENGTH = 40;

  /** 방마다 세는 읽지 않은 멘션 상한. */
  private static final int COUNT_LIMIT = 50;

  private final ChatRoomRepository roomRepository;
  private final ChatMessageRepository messageRepository;
  private final ChatReadMarkRepository readMarkRepository;
  private final ChatMembership membership;

  public ChatMentionHomeCard(
      ChatRoomRepository roomRepository,
      ChatMessageRepository messageRepository,
      ChatReadMarkRepository readMarkRepository,
      ChatMembership membership) {
    this.roomRepository = roomRepository;
    this.messageRepository = messageRepository;
    this.readMarkRepository = readMarkRepository;
    this.membership = membership;
  }

  @Override
  public HomeCardKey key() {
    return HomeCardKey.CHAT_MENTIONS;
  }

  @Override
  public HomeContribution contribute(CurrentUser user, Business business) {
    if (user.organizationType() != OrganizationType.FINANCE_ORGANIZATION
        || !user.businesses().contains(business)) {
      return null;
    }
    Map<String, CurrentUser> accounts = membership.activeAccounts();
    List<ChatMessage> unread = new ArrayList<>();
    for (ChatRoom room : roomRepository.findByBusinessOrderByTypeAscIdAsc(business)) {
      if (!membership.participants(room, accounts).containsKey(user.email())) {
        continue;
      }
      Long mark =
          readMarkRepository
              .findByRoomIdAndEmail(room.getId(), user.email())
              .map(ChatReadMark::getLastReadMessageId)
              .orElse(0L);
      unread.addAll(
          messageRepository.findUnreadMentions(
              room.getId(),
              mark,
              user.email(),
              user.organizationCode(),
              PageRequest.of(0, COUNT_LIMIT)));
    }
    unread.sort(Comparator.comparing(ChatMessage::getId).reversed());
    List<HomeCardItem> items =
        unread.stream()
            .limit(ITEM_COUNT)
            .map(
                m ->
                    new HomeCardItem(
                        preview(m),
                        m.getRoom().getName() + " · " + m.getSenderName(),
                        null,
                        Urgency.NONE,
                        "/" + business.getSlug() + "/chat/" + m.getRoom().getId()))
            .toList();
    return HomeContribution.of(
        new HomeCard(
            key(),
            key().getTitle(),
            HomeCardSource.REAL,
            Urgency.NONE,
            "읽지 않은 멘션 " + unread.size() + "건",
            items,
            "/" + business.getSlug() + "/" + key().getFeature()));
  }

  private static String preview(ChatMessage message) {
    String content = message.getContent().replaceAll("\\s+", " ");
    if (content.isEmpty()) {
      return "(파일)";
    }
    return content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) + "…" : content;
  }
}
