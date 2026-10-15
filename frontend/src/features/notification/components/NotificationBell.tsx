import {
  ActionIcon,
  Anchor,
  Button,
  Divider,
  Group,
  Indicator,
  Popover,
  Stack,
  Text,
} from '@mantine/core';
import { IconBell } from '@tabler/icons-react';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { DEFAULT_BUSINESS_SLUG } from '../../../app/navigation';
import {
  useMarkAllRead,
  useMarkRead,
  useNotifications,
  useUnreadCount,
  type NotificationResponse,
} from '../hooks';
import { NotificationItem } from './NotificationItem';

/** 상단 바의 알림 벨. 읽지 않은 알림이 있으면 빨간 점, 누르면 최근 읽지 않은 알림 5개와 알림 센터 링크. */
export function NotificationBell() {
  const [opened, setOpened] = useState(false);
  const navigate = useNavigate();
  const { business } = useParams<{ business: string }>();
  const { data: unread } = useUnreadCount();
  const { data: recent } = useNotifications({ page: 0, size: 5, unreadOnly: true });
  const markRead = useMarkRead();
  const markAllRead = useMarkAllRead();

  const count = unread?.count ?? 0;
  const centerPath = `/${business ?? DEFAULT_BUSINESS_SLUG}/notifications`;

  const open = (n: NotificationResponse) => {
    setOpened(false);
    if (!n.read) markRead.mutate(n.id);
    navigate(n.link ?? centerPath);
  };

  return (
    <Popover opened={opened} onChange={setOpened} width={380} position="bottom-end" shadow="card">
      <Popover.Target>
        <Indicator color="red" size={9} offset={6} disabled={count === 0} withBorder>
          <ActionIcon
            variant="subtle"
            color="gray"
            aria-label={count > 0 ? `알림 ${count}건` : '알림'}
            onClick={() => setOpened((o) => !o)}
          >
            <IconBell size={20} color="white" />
          </ActionIcon>
        </Indicator>
      </Popover.Target>
      <Popover.Dropdown p="xs">
        <Group justify="space-between" px="xs" pb="xs">
          <Text fw={700}>읽지 않은 알림 {count > 0 ? `${count}건` : ''}</Text>
          <Button
            size="compact-xs"
            variant="subtle"
            disabled={count === 0}
            loading={markAllRead.isPending}
            onClick={() => markAllRead.mutate()}
          >
            모두 읽음
          </Button>
        </Group>
        <Divider />
        <Stack gap={2} py="xs">
          {recent?.content.length ? (
            recent.content.map((n) => (
              <NotificationItem key={n.id} notification={n} onOpen={open} compact />
            ))
          ) : (
            <Text size="sm" c="dimmed" ta="center" py="md">
              읽지 않은 알림이 없습니다.
            </Text>
          )}
        </Stack>
        <Divider />
        <Group justify="center" pt="xs">
          <Anchor component={Link} to={centerPath} size="sm" onClick={() => setOpened(false)}>
            알림 센터에서 모두 보기
          </Anchor>
        </Group>
      </Popover.Dropdown>
    </Popover>
  );
}
