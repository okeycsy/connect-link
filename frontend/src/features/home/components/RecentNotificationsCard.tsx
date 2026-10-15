import {
  Badge,
  Button,
  Box,
  Card,
  Group,
  Skeleton,
  Stack,
  Text,
  UnstyledButton,
} from '@mantine/core';
import { IconChevronRight } from '@tabler/icons-react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { DEFAULT_BUSINESS_SLUG } from '../../../app/navigation';
import {
  useMarkRead,
  useNotifications,
  useUnreadCount,
  type NotificationResponse,
} from '../../notification/hooks';
import { formatDateTime } from '../../../shared/format';
import classes from './RecentNotificationsCard.module.css';

/** 홈에는 최근 4건만 보이고 나머지는 더보기로 알림 센터에서 본다. SHY 요청. */
const SIZE = 4;

/**
 * 홈 왼쪽 큰 칸: 최근 알림. 알림 센터(/api/v1/notifications)와 같은 내용을 시안 B 의 "최근 소식" 모양으로 보인다.
 * 최근 4건만 보이고 아래 더보기는 알림 센터로 간다.
 * 한 줄 = 둥근 사각 아바타(알림 종류 첫 글자) + 종류·업무·시각 + 제목·본문. 누르면 읽음 처리하고 연결된 화면으로 간다.
 */
export function RecentNotificationsCard() {
  const navigate = useNavigate();
  const { business } = useParams<{ business: string }>();
  const { data, isPending, isError } = useNotifications({ page: 0, size: SIZE });
  const { data: unread } = useUnreadCount();
  const markRead = useMarkRead();
  const centerPath = `/${business ?? DEFAULT_BUSINESS_SLUG}/notifications`;
  const unreadCount = unread?.count ?? 0;

  const open = (n: NotificationResponse) => {
    if (!n.read) markRead.mutate(n.id);
    navigate(n.link ?? centerPath);
  };

  return (
    <Card className={classes.card} h="100%" data-testid="home-recent-notifications">
      <Group align="center" mb="xs" px={16} wrap="nowrap">
        <Group gap="xs">
          <Text fw={700} size="xl">
            최근 소식
          </Text>
          {unreadCount > 0 && (
            <Badge size="sm" variant="filled" color="red">
              읽지 않음 {unreadCount}
            </Badge>
          )}
        </Group>
      </Group>

      {isError && (
        <Text c="red" size="sm" px={16}>
          알림을 불러오지 못했습니다.
        </Text>
      )}
      {isPending ? (
        <Stack gap="xs" px={16}>
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} h={52} radius="lg" />
          ))}
        </Stack>
      ) : data?.content.length ? (
        <Stack gap={2}>
          {data.content.map((n) => (
            <UnstyledButton
              key={n.id}
              className={classes.row}
              data-unread={!n.read || undefined}
              onClick={() => open(n)}
              aria-label={n.title}
            >
              <Box className={classes.avatar} aria-hidden>
                {n.typeLabel.slice(0, 1)}
                {!n.read && <span className={classes.unreadDot} />}
              </Box>
              <Box style={{ flex: 1, minWidth: 0 }}>
                <Group gap={8} align="baseline" wrap="wrap">
                  <Text size="md" fw={700}>
                    {n.typeLabel}
                  </Text>
                  {n.businessLabel && (
                    <Text size="sm" fw={600} c="clay.6">
                      # {n.businessLabel}
                    </Text>
                  )}
                  <Text size="xs" c="dimmed">
                    {formatDateTime(n.createdAt)}
                  </Text>
                </Group>
                <Text size="md" c="var(--cl-ink-soft)" fw={n.read ? 400 : 600} lh={1.55}>
                  {n.title}
                </Text>
                {n.body && (
                  <Text size="sm" c="dimmed" lineClamp={1}>
                    {n.body}
                  </Text>
                )}
              </Box>
            </UnstyledButton>
          ))}
        </Stack>
      ) : (
        <Text c="dimmed" ta="center" py="xl">
          새 소식이 없습니다.
        </Text>
      )}

      <Group justify="center" mt="sm">
        <Button
          component={Link}
          to={centerPath}
          variant="light"
          size="sm"
          rightSection={<IconChevronRight size={16} />}
        >
          더보기
        </Button>
      </Group>
    </Card>
  );
}
