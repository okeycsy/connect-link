package com.connectlink.chat;

import com.connectlink.account.AccountService;
import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.OrganizationType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 채팅방 구성원 계산. 업무 단체방은 그 업무를 맡은 사용 중 계정에 따로 초대한 사람을 더하고, 초대방은 초대한 사람이다. 첨부 권한 빈({@link
 * ChatAttachmentAccess})도 쓰므로 첨부 모듈에 기대지 않게 서비스와 따로 둔다.
 */
@Component
class ChatMembership {

  private final AccountService accountService;

  ChatMembership(AccountService accountService) {
    this.accountService = accountService;
  }

  /** 방 구성원 한 명. 업무 단체방의 업무 담당자는 invited 가 false 다. */
  record Participant(
      String email,
      String name,
      String organizationCode,
      String organizationName,
      OrganizationType organizationType,
      String roleLabel,
      boolean invited) {

    boolean kftc() {
      return organizationType == OrganizationType.KFTC;
    }
  }

  /** 사용 중 계정(이메일 → 사용자). 이용기관은 소통 채널을 쓰지 않아 뺀다. */
  Map<String, CurrentUser> activeAccounts() {
    return accountService.listActive().stream()
        .filter(u -> u.organizationType() != OrganizationType.USE_ORGANIZATION)
        .collect(
            Collectors.toMap(
                CurrentUser::email, Function.identity(), (a, b) -> a, LinkedHashMap::new));
  }

  /** 방 구성원(이메일 → 구성원). 초대한 사람 중 지금 사용 중이 아닌 계정은 뺀다. */
  Map<String, Participant> participants(ChatRoom room, Map<String, CurrentUser> accounts) {
    Map<String, Participant> result = new LinkedHashMap<>();
    if (room.getType() == ChatRoomType.BUSINESS) {
      accounts.values().stream()
          .filter(u -> u.businesses().contains(room.getBusiness()))
          .forEach(u -> result.put(u.email(), participantOf(u, false)));
    }
    for (ChatRoomMember member : room.getMembers()) {
      CurrentUser account = accounts.get(member.getEmail());
      if (account != null) {
        result.putIfAbsent(member.getEmail(), participantOf(account, true));
      }
    }
    return result;
  }

  Map<String, Participant> participants(ChatRoom room) {
    return participants(room, activeAccounts());
  }

  boolean isMember(CurrentUser user, ChatRoom room) {
    return participants(room).containsKey(user.email());
  }

  private static Participant participantOf(CurrentUser u, boolean invited) {
    return new Participant(
        u.email(),
        u.name(),
        u.organizationCode(),
        u.organizationName(),
        u.organizationType(),
        u.roleLabel(),
        invited);
  }
}
