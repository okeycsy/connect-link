import {
  Anchor,
  Badge,
  Box,
  Card,
  Group,
  Skeleton,
  Stack,
  Text,
  UnstyledButton,
} from '@mantine/core';
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

/** 넉넉히 받아 두고 카드 높이에 들어가는 만큼만 보인다(잘린 줄은 CSS 가 통째로 숨긴다). */
const SIZE = 12;

/**
 * 홈 왼쪽 큰 칸: 최근 알림. 알림 센터(/api/v1/notifications)와 같은 내용을 시안 B 의 "최근 소식" 모양으로 보인다.
 * 카드는 오른쪽 열과 같은 높이이고, 그 높이에 온전히 들어가는 줄만 보여 빈 공간을 남기지 않는다(최소 4건).
 * 오른쪽 아래 작은 "더보기" 글자 링크가 알림 센터로 간다.
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
        <div className={classes.list}>
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
        </div>
      ) : (
        <Text c="dimmed" ta="center" py="xl">
          새 소식이 없습니다.
        </Text>
      )}

      <Group justify="flex-end" mt="auto" pt={4} px={16}>
        <Anchor component={Link} to={centerPath} size="xs" c="dimmed">
          더보기
        </Anchor>
      </Group>
    </Card>
  );
}
