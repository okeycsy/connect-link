package com.connectlink.chat;

import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.attachment.AttachmentService;
import com.connectlink.attachment.dto.AttachmentResponse;
import com.connectlink.auth.CurrentUser;
import com.connectlink.chat.ChatMembership.Participant;
import com.connectlink.chat.dto.ChatAckResponse;
import com.connectlink.chat.dto.ChatInviteCandidateResponse;
import com.connectlink.chat.dto.ChatMemberResponse;
import com.connectlink.chat.dto.ChatMentionResponse;
import com.connectlink.chat.dto.ChatMessageResponse;
import com.connectlink.chat.dto.ChatMessageSendRequest;
import com.connectlink.chat.dto.ChatRoomCreateRequest;
import com.connectlink.chat.dto.ChatRoomDetailResponse;
import com.connectlink.chat.dto.ChatRoomPermissions;
import com.connectlink.chat.dto.ChatRoomSummaryResponse;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.organization.Business;
import com.connectlink.organization.OrganizationService;
import com.connectlink.organization.OrganizationType;
import com.connectlink.organization.dto.OrganizationResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마. 소통 채널: 업무 단체방과 초대방, 메시지(멘션·확인 공감·첨부), 읽음 표시. 새 메시지는 화면이 몇 초마다 다시 물어 받는다(docs/decisions.md
 * 2026-10-21). 업무 단체방 구성원은 그 업무를 맡은 사용 중 계정이라 요청마다 계정 모듈에서 읽는다.
 */
@Service
@Transactional
public class ChatService {

  /** 결제원 멘션의 기관 코드와 표기. */
  static final String KFTC_CODE = "KFTC";

  static final String KFTC_LABEL = "결제원";

  /** 방 목록의 마지막 메시지 미리보기 길이. */
  private static final int PREVIEW_LENGTH = 60;

  private final ChatRoomRepository roomRepository;
  private final ChatRoomMemberRepository memberRepository;
  private final ChatMessageRepository messageRepository;
  private final ChatReadMarkRepository readMarkRepository;
  private final ChatAccessPolicy accessPolicy;
  private final ChatMembership membership;
  private final OrganizationService organizationService;
  private final AttachmentService attachmentService;
  private final ChatProperties properties;
  private final Clock clock;

  public ChatService(
      ChatRoomRepository roomRepository,
      ChatRoomMemberRepository memberRepository,
      ChatMessageRepository messageRepository,
      ChatReadMarkRepository readMarkRepository,
      ChatAccessPolicy accessPolicy,
      ChatMembership membership,
      OrganizationService organizationService,
      AttachmentService attachmentService,
      ChatProperties properties,
      Clock clock) {
    this.roomRepository = roomRepository;
    this.memberRepository = memberRepository;
    this.messageRepository = messageRepository;
    this.readMarkRepository = readMarkRepository;
    this.accessPolicy = accessPolicy;
    this.membership = membership;
    this.organizationService = organizationService;
    this.attachmentService = attachmentService;
    this.properties = properties;
    this.clock = clock;
  }

  // ---- 방 ----

  /**
   * 내가 구성원인 방 전부. 업무 밖에서 초대받은 방(예: 대외계)도 보여야 하므로 업무로 거르지 않고, 지금 보는 업무(first)의 방을 먼저 둔다. 업무 안에서는 업무
   * 단체방이 먼저이고 그다음은 만든 순이다.
   */
  @Transactional(readOnly = true)
  public List<ChatRoomSummaryResponse> listRooms(CurrentUser user, Business first) {
    Map<String, CurrentUser> accounts = membership.activeAccounts();
    return roomRepository.findAll().stream()
        .sorted(
            Comparator.comparing((ChatRoom r) -> r.getBusiness() != first)
                .thenComparing(ChatRoom::getBusiness)
                .thenComparing(ChatRoom::getType)
                .thenComparing(ChatRoom::getId))
        .map(room -> Map.entry(room, membership.participants(room, accounts)))
        .filter(e -> e.getValue().containsKey(user.email()))
        .map(e -> toSummary(user, e.getKey(), e.getValue().size()))
        .toList();
  }

  /** 초대방을 만든다. 만든 사람은 저절로 들어가고, 고른 사람은 초대할 수 있는 사람이어야 한다. */
  public ChatRoomDetailResponse createRoom(CurrentUser user, ChatRoomCreateRequest body) {
    accessPolicy.ensureCanManage(user, body.business());
    ChatRoom room =
        new ChatRoom(body.business(), ChatRoomType.CUSTOM, body.name().strip(), user.name());
    room.invite(snapshotOf(user), user.name());
    Map<String, ChatInviteCandidateResponse> candidates = candidatesOf(body.business());
    for (String email : body.memberEmails()) {
      if (!email.equals(user.email())) {
        room.invite(snapshotOf(requireCandidate(candidates, email)), user.name());
      }
    }
    roomRepository.saveAndFlush(room);
    return toDetail(user, room, membership.participants(room));
  }

  @Transactional(readOnly = true)
  public ChatRoomDetailResponse getRoom(CurrentUser user, Long roomId) {
    ChatRoom room = loadRoom(roomId);
    Map<String, Participant> participants = membership.participants(room);
    accessPolicy.ensureMember(participants.containsKey(user.email()));
    return toDetail(user, room, participants);
  }

  /** 이 방에 초대할 수 있는 사람(아직 구성원이 아닌 사람). 그 업무 참가기관 담당자와 결제원 사람이다. */
  @Transactional(readOnly = true)
  public List<ChatInviteCandidateResponse> inviteCandidates(CurrentUser user, Long roomId) {
    ChatRoom room = loadRoom(roomId);
    Map<String, Participant> participants = membership.participants(room);
    ensureCanInvite(user, room, participants);
    return candidatesOf(room.getBusiness()).values().stream()
        .filter(c -> !participants.containsKey(c.email()))
        .toList();
  }

  public ChatRoomDetailResponse invite(CurrentUser user, Long roomId, List<String> emails) {
    ChatRoom room = loadRoom(roomId);
    Map<String, CurrentUser> accounts = membership.activeAccounts();
    ensureCanInvite(user, room, membership.participants(room, accounts));
    Map<String, ChatInviteCandidateResponse> candidates = candidatesOf(room.getBusiness());
    for (String email : emails) {
      room.invite(snapshotOf(requireCandidate(candidates, email)), user.name());
    }
    roomRepository.flush();
    return toDetail(user, room, membership.participants(room, accounts));
  }

  /** 따로 초대받은 방에서 나간다. 업무 단체방의 업무 담당자는 나가지 못한다(409 CHAT_CANNOT_LEAVE). */
  public void leave(CurrentUser user, Long roomId) {
    ChatRoom room = loadRoom(roomId);
    Map<String, Participant> participants = membership.participants(room);
    accessPolicy.ensureMember(participants.containsKey(user.email()));
    if (!accessPolicy.canLeave(user, room, true)) {
      throw BusinessException.conflict("CHAT_CANNOT_LEAVE", "업무 단체방은 그 업무를 맡은 동안 나갈 수 없습니다.");
    }
    room.removeMember(user.email());
    readMarkRepository
        .findByRoomIdAndEmail(roomId, user.email())
        .ifPresent(readMarkRepository::delete);
  }

  // ---- 메시지 ----

  /** 최근 메시지부터 한 쪽. 화면은 뒤집어 아래에 최신을 둔다. */
  @Transactional(readOnly = true)
  public PageResponse<ChatMessageResponse> messages(
      CurrentUser user, Long roomId, Pageable pageable) {
    ChatRoom room = loadRoom(roomId);
    Map<String, Participant> participants = membership.participants(room);
    accessPolicy.ensureMember(participants.containsKey(user.email()));
    Page<ChatMessage> page = messageRepository.findByRoom_IdOrderByIdDesc(roomId, pageable);
    Map<String, Long> marks = readMarks(roomId);
    Map<String, String> labels = organizationLabels();
    Map<Long, List<AttachmentResponse>> attachments =
        attachmentService.listOf(
            AttachmentOwnerType.CHAT_MESSAGE,
            page.getContent().stream()
                .filter(ChatMessage::isWithAttachments)
                .map(ChatMessage::getId)
                .toList());
    return PageResponse.from(
        page,
        m ->
            toResponse(
                user,
                m,
                participants.keySet(),
                marks,
                labels,
                attachments.getOrDefault(m.getId(), List.of())));
  }

  /** 메시지를 보낸다. 본문이 비었으면 첨부를 올린다고 해야 한다. 보낸 메시지까지 읽은 것으로 둔다. */
  public ChatMessageResponse send(CurrentUser user, Long roomId, ChatMessageSendRequest body) {
    ChatRoom room = loadRoom(roomId);
    Map<String, Participant> participants = membership.participants(room);
    accessPolicy.ensureMember(participants.containsKey(user.email()));
    if (body.content().isEmpty() && !body.withAttachments()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST, "CHAT_MESSAGE_EMPTY", "content", "메시지를 입력하거나 파일을 골라 주세요.");
    }
    Set<String> mentionable =
        mentionables(participants.values()).stream()
            .map(ChatMentionResponse::organizationCode)
            .collect(Collectors.toSet());
    for (String code : body.mentionOrganizationCodes()) {
      if (!mentionable.contains(code)) {
        throw BusinessException.onField(
            HttpStatus.BAD_REQUEST,
            "CHAT_MENTION_INVALID",
            "mentionOrganizationCodes",
            "이 방 구성원의 기관만 멘션할 수 있습니다.");
      }
    }
    ChatMessage message =
        messageRepository.saveAndFlush(
            new ChatMessage(
                room,
                user,
                body.content(),
                body.mentionOrganizationCodes(),
                body.withAttachments()));
    advanceReadMark(roomId, user.email(), message.getId());
    return toResponse(
        user, message, participants.keySet(), readMarks(roomId), organizationLabels(), List.of());
  }

  /** 이 메시지까지 읽었다. 방에 있는 메시지까지만 옮기고 뒤로는 가지 않는다. */
  public void markRead(CurrentUser user, Long roomId, Long lastMessageId) {
    ChatRoom room = loadRoom(roomId);
    accessPolicy.ensureMember(membership.participants(room).containsKey(user.email()));
    messageRepository
        .findFirstByRoom_IdOrderByIdDesc(roomId)
        .map(latest -> Math.min(latest.getId(), lastMessageId))
        .ifPresent(id -> advanceReadMark(roomId, user.email(), id));
  }

  /** 확인 공감을 남긴다(이미 남겼으면 그대로). */
  public ChatMessageResponse acknowledge(CurrentUser user, Long messageId) {
    ChatMessage message = loadMessage(messageId);
    Map<String, Participant> participants = membership.participants(message.getRoom());
    if (!accessPolicy.canAck(user, message, participants.containsKey(user.email()))) {
      accessPolicy.ensureMember(participants.containsKey(user.email()));
      throw BusinessException.conflict("CHAT_ACK_OWN_MESSAGE", "내가 보낸 메시지에는 확인을 남기지 않습니다.");
    }
    message.acknowledge(user);
    messageRepository.flush();
    return reload(user, message, participants);
  }

  /** 남긴 확인 공감을 거둔다. */
  public ChatMessageResponse withdrawAck(CurrentUser user, Long messageId) {
    ChatMessage message = loadMessage(messageId);
    Map<String, Participant> participants = membership.participants(message.getRoom());
    accessPolicy.ensureMember(participants.containsKey(user.email()));
    message.withdrawAck(user.email());
    messageRepository.flush();
    return reload(user, message, participants);
  }

  // ---- 예약 작업·계정 이벤트 ----

  /** 보관 기간이 지난 첨부를 지우고 메시지에 표시한다. 지운 메시지 수를 돌려준다. */
  public int expireAttachments() {
    LocalDateTime cutoff = LocalDateTime.now(clock).minus(properties.getAttachmentRetention());
    List<ChatMessage> expired =
        messageRepository.findByWithAttachmentsTrueAndAttachmentsExpiredFalseAndCreatedAtBefore(
            cutoff);
    for (ChatMessage message : expired) {
      attachmentService.deleteAllOf(AttachmentOwnerType.CHAT_MESSAGE, message.getId());
      message.expireAttachments();
    }
    return expired.size();
  }

  /** 업무마다 단체방이 하나씩 있게 한다. 없는 업무의 방을 만들고 만든 수를 돌려준다. */
  public int ensureBusinessRooms() {
    int created = 0;
    for (Business business : Business.values()) {
      if (roomRepository.findFirstByBusinessAndType(business, ChatRoomType.BUSINESS).isEmpty()) {
        roomRepository.save(ChatRoom.businessRoom(business));
        created++;
      }
    }
    return created;
  }

  /** 탈퇴한 사람을 초대한 방에서 빼고 읽음 표시를 지운다. 보낸 메시지와 확인 공감은 대화 기록이라 남긴다. */
  public void removeEverywhere(String email) {
    for (ChatRoomMember member : memberRepository.findByEmail(email)) {
      member.getRoom().removeMember(email);
    }
    readMarkRepository.deleteByEmail(email);
  }

  // ---- 내부 ----

  private ChatRoom loadRoom(Long id) {
    return roomRepository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."));
  }

  private ChatMessage loadMessage(Long id) {
    return messageRepository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("CHAT_MESSAGE_NOT_FOUND", "메시지를 찾을 수 없습니다."));
  }

  private void ensureCanInvite(
      CurrentUser user, ChatRoom room, Map<String, Participant> participants) {
    boolean member = participants.containsKey(user.email());
    accessPolicy.ensureMember(member);
    if (!accessPolicy.canInvite(user, room, member)) {
      accessPolicy.ensureCanManage(user, room.getBusiness());
    }
  }

  /** 그 업무 참가기관 담당자와 결제원 사람(업무 밖 포함). 결제원이 먼저, 그다음 기관 이름·사람 이름순. */
  private Map<String, ChatInviteCandidateResponse> candidatesOf(Business business) {
    return membership.activeAccounts().values().stream()
        .filter(
            u ->
                u.organizationType() == OrganizationType.KFTC
                    || (u.organizationType() == OrganizationType.FINANCE_ORGANIZATION
                        && u.businesses().contains(business)))
        .sorted(
            Comparator.comparing((CurrentUser u) -> u.organizationType() != OrganizationType.KFTC)
                .thenComparing(CurrentUser::organizationName)
                .thenComparing(CurrentUser::name))
        .map(
            u ->
                new ChatInviteCandidateResponse(
                    u.email(),
                    u.name(),
                    u.organizationCode(),
                    u.organizationName(),
                    u.organizationType() == OrganizationType.KFTC,
                    u.roleLabel()))
        .collect(
            Collectors.toMap(
                ChatInviteCandidateResponse::email,
                Function.identity(),
                (a, b) -> a,
                LinkedHashMap::new));
  }

  private static ChatInviteCandidateResponse requireCandidate(
      Map<String, ChatInviteCandidateResponse> candidates, String email) {
    ChatInviteCandidateResponse candidate = candidates.get(email);
    if (candidate == null) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "CHAT_INVITEE_INVALID",
          "emails",
          "그 업무의 참가기관 담당자나 결제원 사람만 초대할 수 있습니다.");
    }
    return candidate;
  }

  private static ChatRoomMember.Snapshot snapshotOf(CurrentUser u) {
    return new ChatRoomMember.Snapshot(
        u.email(), u.name(), u.organizationCode(), u.organizationName(), u.organizationType());
  }

  private static ChatRoomMember.Snapshot snapshotOf(ChatInviteCandidateResponse c) {
    return new ChatRoomMember.Snapshot(
        c.email(),
        c.name(),
        c.organizationCode(),
        c.organizationName(),
        c.kftc() ? OrganizationType.KFTC : OrganizationType.FINANCE_ORGANIZATION);
  }

  /** 구성원의 기관(결제원 먼저, 그다음 기관 이름순). 멘션할 수 있는 기관이다. */
  private static List<ChatMentionResponse> mentionables(Collection<Participant> participants) {
    Map<String, ChatMentionResponse> result = new LinkedHashMap<>();
    participants.stream()
        .sorted(
            Comparator.comparing((Participant p) -> !p.kftc())
                .thenComparing(Participant::organizationName))
        .forEach(
            p ->
                result.putIfAbsent(
                    p.organizationCode(),
                    new ChatMentionResponse(
                        p.organizationCode(), p.kftc() ? KFTC_LABEL : p.organizationName())));
    return List.copyOf(result.values());
  }

  /** 기관 코드 → 멘션 표기. 결제원은 "결제원" 이다. */
  private Map<String, String> organizationLabels() {
    return organizationService.listActive().stream()
        .collect(
            Collectors.toMap(
                OrganizationResponse::code,
                o -> o.type() == OrganizationType.KFTC ? KFTC_LABEL : o.name(),
                (a, b) -> a));
  }

  private Map<String, Long> readMarks(Long roomId) {
    return readMarkRepository.findByRoomId(roomId).stream()
        .collect(Collectors.toMap(ChatReadMark::getEmail, ChatReadMark::getLastReadMessageId));
  }

  private void advanceReadMark(Long roomId, String email, Long messageId) {
    readMarkRepository
        .findByRoomIdAndEmail(roomId, email)
        .ifPresentOrElse(
            mark -> mark.advanceTo(messageId),
            () -> readMarkRepository.save(new ChatReadMark(roomId, email, messageId)));
  }

  private ChatRoomSummaryResponse toSummary(CurrentUser user, ChatRoom room, int memberCount) {
    Long myMark =
        readMarkRepository
            .findByRoomIdAndEmail(room.getId(), user.email())
            .map(ChatReadMark::getLastReadMessageId)
            .orElse(0L);
    var last = messageRepository.findFirstByRoom_IdOrderByIdDesc(room.getId());
    return new ChatRoomSummaryResponse(
        room.getId(),
        room.getBusiness(),
        room.getType(),
        room.getType().getLabel(),
        room.getName(),
        memberCount,
        last.map(ChatService::preview).orElse(null),
        last.map(ChatMessage::getSenderName).orElse(null),
        last.map(ChatMessage::getCreatedAt).orElse(null),
        messageRepository.countByRoom_IdAndIdGreaterThanAndSenderEmailNot(
            room.getId(), myMark, user.email()),
        messageRepository.existsUnreadMention(
            room.getId(), myMark, user.email(), user.organizationCode()));
  }

  private static String preview(ChatMessage message) {
    String content = message.getContent().replaceAll("\\s+", " ");
    if (content.isEmpty()) {
      return "(파일)";
    }
    return content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) + "…" : content;
  }

  private ChatRoomDetailResponse toDetail(
      CurrentUser user, ChatRoom room, Map<String, Participant> participants) {
    boolean member = participants.containsKey(user.email());
    List<ChatMemberResponse> members =
        participants.values().stream()
            .sorted(
                Comparator.comparing((Participant p) -> !p.kftc())
                    .thenComparing(Participant::organizationName)
                    .thenComparing(Participant::name))
            .map(
                p ->
                    new ChatMemberResponse(
                        p.email(),
                        p.name(),
                        p.organizationCode(),
                        p.organizationName(),
                        p.kftc(),
                        p.roleLabel(),
                        p.invited()))
            .toList();
    return new ChatRoomDetailResponse(
        room.getId(),
        room.getBusiness(),
        room.getType(),
        room.getType().getLabel(),
        room.getName(),
        room.getCreatedByName(),
        members,
        mentionables(participants.values()),
        new ChatRoomPermissions(
            accessPolicy.canInvite(user, room, member), accessPolicy.canLeave(user, room, member)));
  }

  private ChatMessageResponse reload(
      CurrentUser user, ChatMessage message, Map<String, Participant> participants) {
    return toResponse(
        user,
        message,
        participants.keySet(),
        readMarks(message.getRoom().getId()),
        organizationLabels(),
        message.isWithAttachments()
            ? attachmentService
                .listOf(AttachmentOwnerType.CHAT_MESSAGE, List.of(message.getId()))
                .getOrDefault(message.getId(), List.of())
            : List.of());
  }

  /** 읽지 않은 사람 수는 보낸 사람을 뺀 구성원 중 이 메시지 앞까지만 읽은 사람이다. */
  private ChatMessageResponse toResponse(
      CurrentUser user,
      ChatMessage m,
      Set<String> members,
      Map<String, Long> marks,
      Map<String, String> labels,
      List<AttachmentResponse> attachments) {
    long unread =
        members.stream()
            .filter(email -> !email.equals(m.getSenderEmail()))
            .filter(email -> marks.getOrDefault(email, 0L) < m.getId())
            .count();
    return new ChatMessageResponse(
        m.getId(),
        m.getSenderEmail(),
        m.getSenderName(),
        m.getSenderOrganizationCode(),
        m.getSenderOrganizationName(),
        m.getSenderOrganizationType() == OrganizationType.KFTC,
        m.getSenderEmail().equals(user.email()),
        m.getContent(),
        m.getCreatedAt(),
        unread,
        m.getMentions().stream()
            .map(
                n ->
                    new ChatMentionResponse(
                        n.getOrganizationCode(),
                        labels.getOrDefault(n.getOrganizationCode(), n.getOrganizationCode())))
            .toList(),
        m.mentions(user.organizationCode()),
        m.getAcks().stream()
            .map(
                a ->
                    new ChatAckResponse(
                        a.getOrganizationCode(),
                        a.getOrganizationName(),
                        a.getName(),
                        a.getEmail().equals(user.email())))
            .toList(),
        m.isWithAttachments(),
        m.isAttachmentsExpired() ? List.of() : attachments,
        m.isAttachmentsExpired(),
        accessPolicy.canAck(user, m, members.contains(user.email())));
  }
}
