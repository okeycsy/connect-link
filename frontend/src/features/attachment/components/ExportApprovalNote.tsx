import { Group, Text } from '@mantine/core';
import { IconShieldCheck } from '@tabler/icons-react';

/** 결제원 담당자가 파일을 올리는 칸 위의 안내. 결제원 파일은 외부망 반출이라 같은 팀 팀장 결재 뒤 참가기관에 보인다. */
export function ExportApprovalNote() {
  return (
    <Group gap={6} wrap="nowrap" data-testid="export-approval-note">
      <IconShieldCheck size={16} stroke={1.8} color="var(--mantine-color-clay-6)" />
      <Text size="xs" c="dimmed">
        올린 파일은 외부망 반출이라 같은 팀 팀장님이 승인한 뒤에 참가기관에 보여요. 결재 요청은
        저절로 갑니다.
      </Text>
    </Group>
  );
}
