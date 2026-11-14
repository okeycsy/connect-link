import { Group, Text } from '@mantine/core';
import { IconWorldOff } from '@tabler/icons-react';

/** 결제원 담당자가 인터넷망에서 첨부를 쓰려 할 때의 안내. 내부망 전용 안내와 같은 amber 아이콘이다. */
export function FileTransferBlockedNote() {
  return (
    <Group gap={6} wrap="nowrap" data-testid="file-transfer-blocked">
      <IconWorldOff size={16} stroke={1.8} color="var(--mantine-color-amber-5)" />
      <Text size="xs" c="dimmed">
        결제원 담당자는 인터넷망에서 첨부파일을 올리거나 내려받을 수 없어요. 내부망에서 이용해
        주세요.
      </Text>
    </Group>
  );
}
