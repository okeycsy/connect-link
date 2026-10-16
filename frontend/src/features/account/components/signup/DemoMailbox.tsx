import { Badge, Box, Group, Skeleton, Stack, Text } from '@mantine/core';
import { IconInbox } from '@tabler/icons-react';

import { formatDateTime } from '../../../../shared/format';
import { useDemoMailbox } from '../../../notification/hooks';
import classes from './DemoMailbox.module.css';

/**
 * 데모 도구: 받은 메일함. 프로토타입은 메일을 실제로 보내지 않고 발송함에 남기므로, 시연할 때 인증 코드 메일을 여기서 본다.
 * demo 프로필에서만 보인다. 가장 최근 메일 한 통을 보여 준다.
 */
export function DemoMailbox({ email }: { email: string }) {
  const { data, isPending } = useDemoMailbox(email, true);
  const latest = data?.[0];

  return (
    <Box className={classes.box} data-testid="demo-mailbox">
      <Group gap="xs" mb="xs" wrap="nowrap">
        <IconInbox size={18} stroke={1.7} />
        <Text size="sm" fw={600}>
          받은 메일함
        </Text>
        <Badge variant="outline" color="gray" size="sm">
          데모
        </Badge>
        <Text size="xs" c="dimmed" truncate>
          {email}
        </Text>
      </Group>
      {isPending ? (
        <Stack gap={6}>
          <Skeleton h={16} w="60%" />
          <Skeleton h={48} />
        </Stack>
      ) : latest ? (
        <Box className={classes.mail}>
          <Group justify="space-between" gap="xs" wrap="nowrap">
            <Text size="sm" fw={600} truncate>
              {latest.subject}
            </Text>
            <Text size="xs" c="dimmed" className={classes.time}>
              {formatDateTime(latest.createdAt)}
            </Text>
          </Group>
          <Text size="sm" mt={6} className={classes.body}>
            {latest.body}
          </Text>
        </Box>
      ) : (
        <Text size="sm" c="dimmed">
          아직 받은 메일이 없습니다.
        </Text>
      )}
      <Text size="xs" c="dimmed" mt="xs">
        시연용입니다. 운영에서는 이 칸이 없고 회사 메일로 코드가 갑니다.
      </Text>
    </Box>
  );
}
