import { Button, Skeleton, Stack } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import { useCallback, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import classes from '../components/Chat.module.css';
import { ChatConversation } from '../components/ChatConversation';
import { ChatRoomList } from '../components/ChatRoomList';
import { CreateChatRoomModal } from '../components/ChatRoomModals';
import { canCreateChatRoom, useChatRooms } from '../hooks';

/**
 * 마. 참가기관 소통 채널. 왼쪽은 내가 들어 있는 방(업무 단체방, 초대방), 오른쪽은 고른 방의 대화다.
 * 주소 /{업무}/chat/{방 id}. 휴대폰 폭에서는 방을 고르면 대화만 보인다.
 */
export function ChatPage() {
  const { business: slug = '', roomId: roomParam } = useParams<{
    business: string;
    roomId?: string;
  }>();
  const roomId = roomParam ? Number(roomParam) : null;
  const navigate = useNavigate();
  const { data: me } = useMe();
  const current = useCurrentBusiness();
  const rooms = useChatRooms(slug);
  const [createOpen, setCreateOpen] = useState(false);
  const [inviteRoomId, setInviteRoomId] = useState<number | null>(null);
  const clearInvite = useCallback(() => setInviteRoomId(null), []);

  const canCreate = canCreateChatRoom(me, slug);

  return (
    <Stack gap="md">
      <PageHeader
        title="소통 채널"
        description="결제원과 참가기관 담당자가 함께 쓰는 업무 대화방. @기관 으로 부르고, 받은 기관은 확인을 누릅니다."
        actions={
          canCreate && (
            <Button
              leftSection={<IconPlus size={18} stroke={1.8} />}
              onClick={() => setCreateOpen(true)}
            >
              초대방 만들기
            </Button>
          )
        }
      />

      <div className={classes.layout} data-has-room={roomId != null || undefined}>
        <aside className={classes.roomPane} aria-label="채팅방 목록">
          <div className={classes.roomScroll}>
            {rooms.isLoading ? (
              <Stack gap="xs" p="xs">
                {[0, 1, 2].map((i) => (
                  <Skeleton key={i} height={56} radius="lg" />
                ))}
              </Stack>
            ) : rooms.isError ? (
              <ErrorState error={rooms.error} />
            ) : rooms.data && rooms.data.length > 0 ? (
              <ChatRoomList rooms={rooms.data} slug={slug} activeRoomId={roomId} />
            ) : (
              <StateView kind="empty" compact title="들어 있는 방이 없습니다" />
            )}
          </div>
        </aside>

        {roomId != null ? (
          <ChatConversation
            key={roomId}
            roomId={roomId}
            slug={slug}
            inviteOnOpen={inviteRoomId === roomId}
            onInviteOpened={clearInvite}
          />
        ) : (
          <section className={classes.conversation} aria-label="대화">
            <StateView
              kind="empty"
              title="방을 골라 주세요"
              description="왼쪽에서 방을 고르면 대화가 열립니다. 읽지 않은 메시지와 우리 기관을 부른 메시지는 배지로 보입니다."
            />
          </section>
        )}
      </div>

      {canCreate && current && (
        <CreateChatRoomModal
          business={current.code}
          opened={createOpen}
          onClose={() => setCreateOpen(false)}
          onCreated={(room) => {
            setCreateOpen(false);
            setInviteRoomId(room.id);
            navigate(`/${slug}/chat/${room.id}`);
          }}
        />
      )}
    </Stack>
  );
}
