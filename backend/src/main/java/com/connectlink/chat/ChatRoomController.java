package com.connectlink.chat;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.chat.dto.ChatInviteCandidateResponse;
import com.connectlink.chat.dto.ChatInviteRequest;
import com.connectlink.chat.dto.ChatMessageResponse;
import com.connectlink.chat.dto.ChatMessageSendRequest;
import com.connectlink.chat.dto.ChatReadRequest;
import com.connectlink.chat.dto.ChatRoomCreateRequest;
import com.connectlink.chat.dto.ChatRoomDetailResponse;
import com.connectlink.chat.dto.ChatRoomSummaryResponse;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.organization.Business;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 소통 채널: 방 목록·만들기·초대·나가기, 메시지 목록·보내기, 읽음 표시. */
@RestController
@RequestMapping("/api/v1/chat-rooms")
@Tag(name = "소통 채널", description = "업무 단체방과 초대방, 메시지, 멘션, 확인 공감")
public class ChatRoomController {

  private final ChatService chatService;
  private final CurrentUserProvider currentUserProvider;

  public ChatRoomController(ChatService chatService, CurrentUserProvider currentUserProvider) {
    this.chatService = chatService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "채팅방 목록 조회",
      description =
          "한 업무 영역에서 내가 구성원인 방. 업무 단체방이 먼저다. 읽지 않은 메시지 수와 내 기관 멘션 여부가 함께 온다. 화면은 몇 초마다 다시 부른다.")
  public List<ChatRoomSummaryResponse> list(
      @Parameter(description = "먼저 보일 업무 영역 slug (예: account-info)") @RequestParam
          String business) {
    return chatService.listRooms(currentUserProvider.require(), resolveBusiness(business));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "초대방 만들기",
      description =
          "그 업무 결제원 담당자가 만든다. 만든 사람은 저절로 들어가고, memberEmails 는 초대할 수 있는 사람이어야 한다(400 CHAT_INVITEE_INVALID).")
  public ChatRoomDetailResponse create(@Valid @RequestBody ChatRoomCreateRequest request) {
    return chatService.createRoom(currentUserProvider.require(), request);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "채팅방 상세 조회",
      description = "구성원, 멘션할 수 있는 기관, 지금 할 수 있는 일(permissions). 구성원만 본다.")
  public ChatRoomDetailResponse get(@PathVariable Long id) {
    return chatService.getRoom(currentUserProvider.require(), id);
  }

  @GetMapping("/{id}/invite-candidates")
  @Operation(
      summary = "초대할 수 있는 사람 조회",
      description = "그 업무 참가기관 담당자와 결제원 사람(업무 밖 대외계 등) 중 아직 구성원이 아닌 사람. 그 업무 결제원 담당자만 부른다.")
  public List<ChatInviteCandidateResponse> inviteCandidates(@PathVariable Long id) {
    return chatService.inviteCandidates(currentUserProvider.require(), id);
  }

  @PostMapping("/{id}/members")
  @Operation(summary = "채팅방에 초대", description = "그 업무 결제원 담당자가 초대한다. 업무 단체방에도 업무 밖 사람을 더 부를 수 있다.")
  public ChatRoomDetailResponse invite(
      @PathVariable Long id, @Valid @RequestBody ChatInviteRequest request) {
    return chatService.invite(currentUserProvider.require(), id, request.emails());
  }

  @DeleteMapping("/{id}/members/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "채팅방 나가기",
      description = "따로 초대받은 방에서 나간다. 업무 단체방의 업무 담당자는 나가지 못한다(409 CHAT_CANNOT_LEAVE).")
  public void leave(@PathVariable Long id) {
    chatService.leave(currentUserProvider.require(), id);
  }

  @GetMapping("/{id}/messages")
  @Operation(
      summary = "메시지 목록 조회",
      description = "최근 메시지부터 한 쪽. 메시지마다 읽지 않은 사람 수, 멘션, 확인 공감, 첨부가 온다. 이전 메시지는 size 를 늘려 다시 부른다.")
  public PageResponse<ChatMessageResponse> messages(
      @PathVariable Long id,
      @Parameter(description = "0부터 시작") @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return chatService.messages(
        currentUserProvider.require(), id, PageRequest.of(page, Math.min(Math.max(size, 1), 500)));
  }

  @PostMapping("/{id}/messages")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "메시지 보내기",
      description =
          "본문과 멘션할 기관을 보낸다. 파일은 withAttachments 로 보낸 뒤 첨부 API(ownerType CHAT_MESSAGE)로 올린다. 본문이 비었는데 파일도 없으면 400 CHAT_MESSAGE_EMPTY.")
  public ChatMessageResponse send(
      @PathVariable Long id, @Valid @RequestBody ChatMessageSendRequest request) {
    return chatService.send(currentUserProvider.require(), id, request);
  }

  @PostMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "읽음 표시", description = "이 메시지까지 읽었다. 앞으로만 옮긴다.")
  public void read(@PathVariable Long id, @Valid @RequestBody ChatReadRequest request) {
    chatService.markRead(currentUserProvider.require(), id, request.lastMessageId());
  }

  static Business resolveBusiness(String slug) {
    return Business.fromSlug(slug)
        .orElseThrow(
            () -> BusinessException.notFound("BUSINESS_NOT_FOUND", "업무 영역을 찾을 수 없습니다: " + slug));
  }
}
