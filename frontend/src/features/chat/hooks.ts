import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import type { CurrentUserResponse } from '../account/hooks';
import { uploadAttachmentFile } from '../attachment/hooks';

export type ChatRoomSummary = components['schemas']['ChatRoomSummaryResponse'];
export type ChatRoomDetail = components['schemas']['ChatRoomDetailResponse'];
export type ChatMember = components['schemas']['ChatMemberResponse'];
export type ChatMention = components['schemas']['ChatMentionResponse'];
export type ChatMessage = components['schemas']['ChatMessageResponse'];
export type ChatAck = components['schemas']['ChatAckResponse'];
export type ChatInviteCandidate = components['schemas']['ChatInviteCandidateResponse'];
export type ChatRoomCreateRequest = components['schemas']['ChatRoomCreateRequest'];

/** 대화 화면이 새 메시지를 다시 묻는 간격. 웹소켓 없이 몇 초마다 다시 부른다(docs/decisions.md 2026-10-21). */
export const MESSAGE_POLL_MS = 3_000;
/** 방 목록(읽지 않은 수)을 다시 묻는 간격. */
export const ROOM_POLL_MS = 10_000;
/** 처음 불러오는 메시지 수. "이전 메시지 더 보기" 를 누르면 이만큼 늘린다. */
export const MESSAGE_PAGE_SIZE = 50;
/** 메시지 하나에 붙일 수 있는 파일 크기(서버 첨부 모듈 한도와 같다). */
export const CHAT_FILE_MAX_BYTES = 20 * 1024 * 1024;

export const chatKeys = {
  all: ['chat'] as const,
  rooms: (business: string) => ['chat', 'rooms', business] as const,
  room: (id: number) => ['chat', 'room', id] as const,
  messages: (id: number, size: number) => ['chat', 'messages', id, size] as const,
  messagesOf: (id: number) => ['chat', 'messages', id] as const,
  candidates: (id: number) => ['chat', 'candidates', id] as const,
};

/** 이 업무에 초대방을 만들 수 있는지. 관리자와 그 업무 결제원 담당자다. 서버도 같은 규칙으로 막는다. */
export function canCreateChatRoom(me: CurrentUserResponse | undefined, slug: string): boolean {
  if (!me) return false;
  return me.admin || (me.organizationType === 'KFTC' && me.businesses.some((b) => b.slug === slug));
}

/** 내가 구성원인 방 전부. 지금 업무 영역의 방이 먼저 온다. */
export function useChatRooms(business: string) {
  return useQuery({
    queryKey: chatKeys.rooms(business),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/chat-rooms', { params: { query: { business } } })),
    enabled: Boolean(business),
    refetchInterval: ROOM_POLL_MS,
  });
}

export function useChatRoom(id: number | null) {
  return useQuery({
    queryKey: chatKeys.room(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/chat-rooms/{id}', { params: { path: { id: id ?? 0 } } })),
    enabled: id != null,
  });
}

/** 최근 메시지 size 개(최신이 앞). 3초마다 다시 부른다. */
export function useChatMessages(id: number | null, size: number) {
  return useQuery({
    queryKey: chatKeys.messages(id ?? 0, size),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/chat-rooms/{id}/messages', {
          params: { path: { id: id ?? 0 }, query: { page: 0, size } },
        }),
      ),
    enabled: id != null,
    refetchInterval: MESSAGE_POLL_MS,
    placeholderData: keepPreviousData,
  });
}

export function useInviteCandidates(id: number, enabled: boolean) {
  return useQuery({
    queryKey: chatKeys.candidates(id),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/chat-rooms/{id}/invite-candidates', {
          params: { path: { id } },
        }),
      ),
    enabled,
  });
}

export interface ChatSendInput {
  content: string;
  mentionOrganizationCodes: string[];
  files: File[];
}

/** 메시지를 보내고, 파일이 있으면 그 메시지에 올린다. */
export function useSendChatMessage(roomId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ content, mentionOrganizationCodes, files }: ChatSendInput) => {
      const message = unwrap(
        await api.POST('/api/v1/chat-rooms/{id}/messages', {
          params: { path: { id: roomId } },
          body: { content, mentionOrganizationCodes, withAttachments: files.length > 0 },
        }),
      );
      for (const file of files) {
        await uploadAttachmentFile({ ownerType: 'CHAT_MESSAGE', ownerId: message.id }, file);
      }
      return message;
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: chatKeys.messagesOf(roomId) });
      queryClient.invalidateQueries({ queryKey: ['chat', 'rooms'] });
    },
  });
}

/** 이 메시지까지 읽었다고 알린다. 방 목록의 읽지 않은 수가 바로 줄도록 목록도 다시 부른다. */
export function useMarkChatRead(roomId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (lastMessageId: number) => {
      const result = await api.POST('/api/v1/chat-rooms/{id}/read', {
        params: { path: { id: roomId } },
        body: { lastMessageId },
      });
      if (result.error !== undefined) unwrap(result);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['chat', 'rooms'] }),
  });
}

/** "확인" 을 누르거나 거둔다. */
export function useToggleChatAck(roomId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ messageId, acked }: { messageId: number; acked: boolean }) => {
      const params = { params: { path: { id: messageId } } };
      return unwrap(
        acked
          ? await api.DELETE('/api/v1/chat-messages/{id}/ack', params)
          : await api.PUT('/api/v1/chat-messages/{id}/ack', params),
      );
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: chatKeys.messagesOf(roomId) }),
  });
}

export function useCreateChatRoom() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: ChatRoomCreateRequest) =>
      unwrap(await api.POST('/api/v1/chat-rooms', { body })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['chat', 'rooms'] }),
  });
}

export function useInviteChatMembers(roomId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (emails: string[]) =>
      unwrap(
        await api.POST('/api/v1/chat-rooms/{id}/members', {
          params: { path: { id: roomId } },
          body: { emails },
        }),
      ),
    onSuccess: (detail) => {
      queryClient.setQueryData(chatKeys.room(roomId), detail);
      queryClient.invalidateQueries({ queryKey: chatKeys.candidates(roomId) });
      queryClient.invalidateQueries({ queryKey: ['chat', 'rooms'] });
    },
  });
}

export function useLeaveChatRoom(roomId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const result = await api.DELETE('/api/v1/chat-rooms/{id}/members/me', {
        params: { path: { id: roomId } },
      });
      if (result.error !== undefined) unwrap(result);
    },
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: chatKeys.room(roomId) });
      queryClient.invalidateQueries({ queryKey: ['chat', 'rooms'] });
    },
  });
}

/** 같은 기관의 "확인" 을 한 묶음으로 모은다. 말풍선 아래에 "KB국민은행 2" 처럼 보인다. */
export function groupAcksByOrganization(acks: ChatAck[]) {
  const groups = new Map<string, { organizationName: string; names: string[]; mine: boolean }>();
  for (const ack of acks) {
    const group = groups.get(ack.organizationCode) ?? {
      organizationName: ack.organizationName,
      names: [],
      mine: false,
    };
    group.names.push(ack.name);
    group.mine = group.mine || ack.mine;
    groups.set(ack.organizationCode, group);
  }
  return [...groups.entries()].map(([organizationCode, g]) => ({ organizationCode, ...g }));
}

/** 입력 중인 낱말이 "@..." 이면 그 검색어를, 아니면 null. 멘션 고르기 메뉴를 여는 기준이다. */
export function mentionQueryAt(text: string, caret: number): string | null {
  const before = text.slice(0, caret);
  const match = /(?:^|\s)@([^\s@]*)$/.exec(before);
  return match ? match[1] : null;
}

/** 초대 화면의 거르기. 업무는 'ALL' 이면 전부, 직무는 'ALL'/'BUSINESS'/'IT', 전산 세부 직무는 비면 전부다. */
export interface InviteFilter {
  business: ChatInviteCandidate['businesses'][number] | 'ALL';
  side: 'ALL' | 'KFTC' | 'ORGANIZATION';
  jobRole: ChatInviteCandidate['jobRole'] | 'ALL';
  itRoles: ChatInviteCandidate['itRoles'][number][];
}

export function filterInviteCandidates(
  candidates: ChatInviteCandidate[],
  filter: InviteFilter,
): ChatInviteCandidate[] {
  return candidates.filter(
    (c) =>
      (filter.business === 'ALL' || c.businesses.includes(filter.business)) &&
      (filter.side === 'ALL' || c.kftc === (filter.side === 'KFTC')) &&
      (filter.jobRole === 'ALL' || c.jobRole === filter.jobRole) &&
      (filter.jobRole !== 'IT' ||
        filter.itRoles.length === 0 ||
        c.itRoles.some((r) => filter.itRoles.includes(r))),
  );
}

/** 초대 후보를 기관별로 묶는다(서버 순서: 결제원 먼저, 그다음 기관 이름순). 묶음마다 한 번에 고른다. */
export function groupInviteCandidates(candidates: ChatInviteCandidate[]) {
  const groups = new Map<string, { label: string; kftc: boolean; items: ChatInviteCandidate[] }>();
  for (const c of candidates) {
    const group = groups.get(c.organizationCode) ?? {
      label: c.kftc ? '결제원' : c.organizationName,
      kftc: c.kftc,
      items: [],
    };
    group.items.push(c);
    groups.set(c.organizationCode, group);
  }
  return [...groups.entries()].map(([organizationCode, g]) => ({ organizationCode, ...g }));
}
