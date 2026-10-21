import { Avatar, Badge, Group, Stack, Text } from '@mantine/core';
import { IconHash, IconLock } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

import { useBusinesses } from '../../organization/hooks';
import type { ChatRoomSummary } from '../hooks';
import classes from './Chat.module.css';
import { formatChatTime } from './chatFormat';

interface Props {
  rooms: ChatRoomSummary[];
  /** 지금 업무 영역 slug. 이 업무의 방이 맨 위 묶음이다. */
  slug: string;
  activeRoomId: number | null;
}

/** 방 목록. 업무별로 묶고, 읽지 않은 수(빨간 채움)와 내 기관 멘션(@)을 오른쪽에 보인다. */
export function ChatRoomList({ rooms, slug, activeRoomId }: Props) {
  const { data: businesses } = useBusinesses();
  const labelOf = (code: string) => businesses?.find((b) => b.code === code)?.label ?? code;

  // 서버가 지금 업무의 방을 먼저 주므로 나온 순서대로 묶는다.
  const groups: { business: string; rooms: ChatRoomSummary[] }[] = [];
  for (const room of rooms) {
    const last = groups[groups.length - 1];
    if (last && last.business === room.business) last.rooms.push(room);
    else groups.push({ business: room.business, rooms: [room] });
  }

  return (
    <Stack gap={0}>
      {groups.map((group) => (
        <div key={group.business}>
          <Text size="xs" fw={600} c="dimmed" className={classes.roomGroup}>
            {labelOf(group.business)}
          </Text>
          {group.rooms.map((room) => (
            <Link
              key={room.id}
              to={`/${slug}/chat/${room.id}`}
              className={classes.room}
              data-active={room.id === activeRoomId || undefined}
              aria-current={room.id === activeRoomId ? 'page' : undefined}
            >
              <Avatar radius={10} size={36} color={room.type === 'BUSINESS' ? 'clay' : 'sky'}>
                {room.type === 'BUSINESS' ? (
                  <IconHash size={18} stroke={1.8} />
                ) : (
                  <IconLock size={18} stroke={1.8} />
                )}
              </Avatar>
              <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
                <Group gap={6} wrap="nowrap" justify="space-between">
                  <Text size="md" fw={room.unreadCount > 0 ? 700 : 600} truncate>
                    {room.name}
                  </Text>
                  <Text size="xs" c="dimmed" className={classes.meta}>
                    {formatChatTime(room.lastMessageAt)}
                  </Text>
                </Group>
                <Group gap={6} wrap="nowrap" justify="space-between">
                  <Text size="sm" c="dimmed" truncate>
                    {room.lastMessagePreview
                      ? `${room.lastMessageSenderName}: ${room.lastMessagePreview}`
                      : `${room.typeLabel} · ${room.memberCount}명`}
                  </Text>
                  <Group gap={4} wrap="nowrap">
                    {room.unreadMention && (
                      <Badge size="sm" color="amber" aria-label="내 기관 멘션">
                        @
                      </Badge>
                    )}
                    {room.unreadCount > 0 && (
                      <Badge size="sm" color="red" variant="filled" aria-label="읽지 않은 메시지">
                        {room.unreadCount > 99 ? '99+' : room.unreadCount}
                      </Badge>
                    )}
                  </Group>
                </Group>
              </Stack>
            </Link>
          ))}
        </div>
      ))}
    </Stack>
  );
}
