import { Badge, Box, Group, Stack, Text, UnstyledButton } from '@mantine/core';

import { formatDateTime } from '../../../shared/format';
import type { NotificationResponse } from '../hooks';
import classes from './NotificationItem.module.css';

interface Props {
  notification: NotificationResponse;
  onOpen: (notification: NotificationResponse) => void;
  /** 벨 팝오버처럼 좁은 곳에서는 본문을 숨긴다. */
  compact?: boolean;
}

/** 알림 한 줄. 읽지 않은 것은 빨간 점과 굵은 제목으로 구분한다. */
export function NotificationItem({ notification, onOpen, compact = false }: Props) {
  return (
    <UnstyledButton
      className={classes.item}
      data-unread={!notification.read || undefined}
      onClick={() => onOpen(notification)}
      aria-label={notification.title}
    >
      <Group align="flex-start" gap="sm" wrap="nowrap">
        <Box className={classes.dot} aria-hidden />
        <Stack gap={4} style={{ flex: 1, minWidth: 0 }}>
          <Group gap={6}>
            <Badge size="sm">{notification.typeLabel}</Badge>
            {notification.businessLabel && (
              <Badge size="sm" variant="outline" color="gray">
                {notification.businessLabel}
              </Badge>
            )}
          </Group>
          <Text size="md" fw={notification.read ? 500 : 700} lineClamp={compact ? 2 : undefined}>
            {notification.title}
          </Text>
          {!compact && notification.body && (
            <Text size="sm" c="dimmed">
              {notification.body}
            </Text>
          )}
          <Text size="xs" c="dimmed">
            {formatDateTime(notification.createdAt)}
          </Text>
        </Stack>
      </Group>
    </UnstyledButton>
  );
}
