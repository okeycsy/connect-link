import { Button, Card, Chip, Group, Pagination, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { NotificationItem } from '../components/NotificationItem';
import {
  useMarkAllRead,
  useMarkRead,
  useNotifications,
  useUnreadCount,
  type NotificationResponse,
} from '../hooks';

const PAGE_SIZE = 20;

/** 알림 센터. 가입 승인, 의견 요청, 연락처 변경, 일시중단 알림을 한 목록으로 본다. 메일·매터모스트로 나간 내용과 같다. */
export function NotificationCenterPage() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<'all' | 'unread'>('all');
  const [page, setPage] = useState(0);
  const unreadOnly = filter === 'unread';
  const { data, isPending, isError } = useNotifications({ page, size: PAGE_SIZE, unreadOnly });
  const { data: unread } = useUnreadCount();
  const markRead = useMarkRead();
  const markAllRead = useMarkAllRead();

  const open = (n: NotificationResponse) => {
    if (!n.read) markRead.mutate(n.id);
    if (n.link) navigate(n.link);
  };

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-end">
        <div>
          <Title order={2}>알림 센터</Title>
          {unread ? (
            <Text c="dimmed" size="lg">
              읽지 않은 알림 {unread.count}건
            </Text>
          ) : (
            // 세는 동안 0건으로 보이지 않게 자리만 둔다.
            <Skeleton h={22} w={140} mt={4} />
          )}
        </div>
        <Button
          variant="light"
          disabled={(unread?.count ?? 0) === 0}
          loading={markAllRead.isPending}
          onClick={() => markAllRead.mutate()}
        >
          모두 읽음
        </Button>
      </Group>

      <Card data-testid="notification-list">
        <Group justify="space-between" mb="sm">
          <Chip.Group
            value={filter}
            onChange={(v) => {
              setFilter(v as 'all' | 'unread');
              setPage(0);
            }}
          >
            <Group gap="xs">
              <Chip value="all" size="xs">
                전체
              </Chip>
              <Chip value="unread" size="xs">
                읽지 않음
              </Chip>
            </Group>
          </Chip.Group>
          {data && (
            <Text size="sm" c="dimmed">
              {data.totalElements}건
            </Text>
          )}
        </Group>

        {isError && (
          <Text c="red" size="sm">
            알림을 불러오지 못했습니다.
          </Text>
        )}
        {isPending ? (
          <Stack gap="xs">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} h={56} />
            ))}
          </Stack>
        ) : data?.content.length ? (
          <Stack gap={2}>
            {data.content.map((n) => (
              <NotificationItem key={n.id} notification={n} onOpen={open} />
            ))}
          </Stack>
        ) : (
          <Text c="dimmed" ta="center" py="xl">
            {unreadOnly ? '읽지 않은 알림이 없습니다.' : '알림이 없습니다.'}
          </Text>
        )}

        {data && data.totalPages > 1 && (
          <Group justify="center" mt="md">
            <Pagination
              total={data.totalPages}
              value={page + 1}
              onChange={(p) => setPage(p - 1)}
              size="sm"
            />
          </Group>
        )}
      </Card>
    </Stack>
  );
}
