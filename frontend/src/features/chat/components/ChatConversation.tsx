import { ActionIcon, Badge, Button, Group, Loader, Stack, Text, Tooltip } from '@mantine/core';
import { IconArrowLeft, IconDoorExit, IconUserPlus, IconUsers } from '@tabler/icons-react';
import dayjs from 'dayjs';
import { Fragment, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ConfirmModal } from '../../../shared/ConfirmModal';
import { formatDate } from '../../../shared/format';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import {
  MESSAGE_PAGE_SIZE,
  type ChatMessage,
  useChatMessages,
  useChatRoom,
  useLeaveChatRoom,
  useMarkChatRead,
  useSendChatMessage,
  useToggleChatAck,
} from '../hooks';
import classes from './Chat.module.css';
import { ChatComposer } from './ChatComposer';
import { ChatMessageItem } from './ChatMessageItem';
import { ChatMembersModal, InviteChatMembersModal } from './ChatRoomModals';

const DAY_LABEL = ['일', '월', '화', '수', '목', '금', '토'];

/** 한 방의 대화. 3초마다 새 메시지를 묻고, 맨 아래가 보이면 읽음을 알린다. */
export function ChatConversation({
  roomId,
  slug,
  inviteOnOpen,
  onInviteOpened,
}: {
  roomId: number;
  slug: string;
  /** 방을 막 만든 뒤라 초대 창을 바로 연다. */
  inviteOnOpen: boolean;
  onInviteOpened: () => void;
}) {
  const navigate = useNavigate();
  const [size, setSize] = useState(MESSAGE_PAGE_SIZE);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [membersOpen, setMembersOpen] = useState(false);
  const [leaveOpen, setLeaveOpen] = useState(false);

  const room = useChatRoom(roomId);
  const messages = useChatMessages(roomId, size);
  const send = useSendChatMessage(roomId);
  const markRead = useMarkChatRead(roomId);
  const ack = useToggleChatAck(roomId);
  const leave = useLeaveChatRoom(roomId);

  const scrollRef = useRef<HTMLDivElement>(null);
  const stickToBottom = useRef(true);
  const lastMarked = useRef(0);
  const keepOffsetFrom = useRef<number | null>(null);

  // 서버는 최신부터 준다. 화면은 오래된 것이 위다.
  const ordered = [...(messages.data?.content ?? [])].reverse();
  const newestId = ordered.length ? ordered[ordered.length - 1].id : 0;

  useEffect(() => {
    if (inviteOnOpen && room.data?.permissions.invite) {
      setInviteOpen(true);
      onInviteOpened();
    }
  }, [inviteOnOpen, room.data, onInviteOpened]);

  // 맨 아래에 붙어 있으면 새 메시지가 와도 맨 아래를 보인다. "이전 메시지" 를 불러왔으면 보던 자리를 지킨다.
  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    if (keepOffsetFrom.current != null) {
      el.scrollTop = el.scrollHeight - keepOffsetFrom.current;
      keepOffsetFrom.current = null;
    } else if (stickToBottom.current) {
      el.scrollTop = el.scrollHeight;
    }
  }, [newestId, ordered.length]);

  // 맨 아래까지 보았으면 읽음을 알린다(앞으로만 옮긴다).
  const { mutate: mark } = markRead;
  useEffect(() => {
    if (newestId > lastMarked.current && stickToBottom.current && !document.hidden) {
      lastMarked.current = newestId;
      mark(newestId);
    }
  }, [newestId, mark]);

  const onScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    stickToBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
    if (stickToBottom.current && newestId > lastMarked.current) {
      lastMarked.current = newestId;
      mark(newestId);
    }
  };

  const loadOlder = () => {
    const el = scrollRef.current;
    if (el) keepOffsetFrom.current = el.scrollHeight - el.scrollTop;
    setSize((s) => s + MESSAGE_PAGE_SIZE);
  };

  const onToggleAck = (message: ChatMessage, acked: boolean) =>
    ack.mutate({ messageId: message.id, acked });

  if (room.isError) {
    return (
      <div className={classes.conversation}>
        <ErrorState error={room.error} homeLink={`/${slug}/chat`} />
      </div>
    );
  }

  const detail = room.data;

  return (
    <section className={classes.conversation} aria-label="대화">
      <Group className={classes.conversationHead} justify="space-between" wrap="nowrap" gap="sm">
        <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
          <ActionIcon
            component={Link}
            to={`/${slug}/chat`}
            variant="subtle"
            hiddenFrom="sm"
            aria-label="방 목록으로"
          >
            <IconArrowLeft size={20} stroke={1.8} />
          </ActionIcon>
          <Stack gap={0} style={{ minWidth: 0 }}>
            <Group gap={6} wrap="nowrap">
              <Text fw={700} size="xl" truncate component="h2">
                {detail?.name ?? ' '}
              </Text>
              {detail && (
                <Badge color={detail.type === 'BUSINESS' ? 'clay' : 'sky'} visibleFrom="sm">
                  {detail.typeLabel}
                </Badge>
              )}
            </Group>
            {detail && (
              <Text size="sm" c="dimmed" truncate>
                {detail.type === 'BUSINESS'
                  ? '이 업무의 결제원·참가기관 담당자가 모두 들어 있습니다.'
                  : `${detail.createdByName} 님이 만든 방`}
              </Text>
            )}
          </Stack>
        </Group>
        {detail && (
          <Group gap={6} wrap="nowrap">
            <Tooltip label="구성원 보기">
              <Button
                variant="outline"
                size="xs"
                leftSection={<IconUsers size={16} stroke={1.8} />}
                onClick={() => setMembersOpen(true)}
              >
                {detail.members.length}
              </Button>
            </Tooltip>
            {detail.permissions.invite && (
              <Button
                variant="light"
                size="xs"
                leftSection={<IconUserPlus size={16} stroke={1.8} />}
                onClick={() => setInviteOpen(true)}
              >
                초대
              </Button>
            )}
            {detail.permissions.leave && (
              <Tooltip label="방 나가기">
                <ActionIcon
                  variant="light"
                  color="red"
                  size={30}
                  aria-label="방 나가기"
                  onClick={() => setLeaveOpen(true)}
                >
                  <IconDoorExit size={16} stroke={1.8} />
                </ActionIcon>
              </Tooltip>
            )}
          </Group>
        )}
      </Group>

      <div
        className={classes.messages}
        ref={scrollRef}
        onScroll={onScroll}
        data-testid="chat-messages"
      >
        {messages.isLoading ? (
          <Group justify="center" py="xl">
            <Loader size="sm" />
          </Group>
        ) : messages.isError ? (
          <ErrorState error={messages.error} />
        ) : ordered.length === 0 ? (
          <StateView
            kind="empty"
            compact
            title="아직 메시지가 없습니다"
            description="첫 메시지를 남겨 보세요."
          />
        ) : (
          <>
            {messages.data && !messages.data.last && (
              <Group justify="center" mb="sm">
                <Button
                  variant="subtle"
                  size="xs"
                  onClick={loadOlder}
                  loading={messages.isFetching}
                >
                  이전 메시지 더 보기
                </Button>
              </Group>
            )}
            {ordered.map((message, i) => {
              const prev = ordered[i - 1];
              const newDay = !prev || !dayjs(prev.sentAt).isSame(message.sentAt, 'day');
              const continued =
                !newDay &&
                prev.senderEmail === message.senderEmail &&
                dayjs(message.sentAt).diff(prev.sentAt, 'minute') < 5;
              return (
                <Fragment key={message.id}>
                  {newDay && (
                    <div className={classes.daySeparator}>
                      <Badge color="gray" variant="outline">
                        {formatDate(message.sentAt)} ({DAY_LABEL[dayjs(message.sentAt).day()]})
                      </Badge>
                    </div>
                  )}
                  <ChatMessageItem
                    message={message}
                    continued={continued}
                    onToggleAck={onToggleAck}
                    ackPending={ack.isPending && ack.variables?.messageId === message.id}
                  />
                </Fragment>
              );
            })}
          </>
        )}
      </div>

      <ChatComposer
        mentionables={detail?.mentionables ?? []}
        sending={send.isPending}
        error={send.error?.message}
        onSend={(input) => {
          stickToBottom.current = true;
          return send.mutateAsync(input);
        }}
      />

      {detail && (
        <>
          <ChatMembersModal
            members={detail.members}
            opened={membersOpen}
            onClose={() => setMembersOpen(false)}
          />
          {detail.permissions.invite && (
            <InviteChatMembersModal
              roomId={roomId}
              business={detail.business}
              opened={inviteOpen}
              onClose={() => setInviteOpen(false)}
            />
          )}
          <ConfirmModal
            opened={leaveOpen}
            onClose={() => setLeaveOpen(false)}
            title="방에서 나갈까요?"
            message="나가면 이 방의 메시지를 더 볼 수 없습니다. 다시 들어오려면 결제원 담당자가 초대해야 합니다."
            confirmLabel="나가기"
            danger
            pending={leave.isPending}
            error={leave.error}
            onConfirm={() =>
              leave.mutate(undefined, { onSuccess: () => navigate(`/${slug}/chat`) })
            }
          />
        </>
      )}
    </section>
  );
}
