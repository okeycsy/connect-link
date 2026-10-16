import { Badge, Group, Skeleton, Stack, Table, Text, Timeline } from '@mantine/core';
import { IconPencil, IconPlus, IconTrash } from '@tabler/icons-react';

import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import { formatDateTime } from '../../../shared/format';
import { useChangeHistories, type ChangeHistory, type HistoryTargetType } from '../hooks';

const ACTION_ICON = { CREATED: IconPlus, UPDATED: IconPencil, DELETED: IconTrash } as const;

interface Props {
  targetType: HistoryTargetType;
  targetId: number | null;
  /** 보여 줄 최대 건수. 연락처처럼 "이전 담당자 한 명까지" 규칙이 있으면 여기서 줄인다. */
  size?: number;
  emptyMessage?: string;
}

/** 대상 하나의 변경 이력 타임라인. 항목마다 변경 전·후 값을 표로 보여 준다. 마스킹은 서버가 이미 한 값이다. */
export function ChangeHistoryList({
  targetType,
  targetId,
  size = 20,
  emptyMessage = '아직 변경 이력이 없습니다.',
}: Props) {
  const { data, isPending, error } = useChangeHistories(targetType, targetId, size);

  if (error) {
    return <StateView kind={stateKindOf(error)} compact />;
  }
  if (isPending || !data) {
    return (
      <Stack gap="sm" data-testid="change-history-loading">
        <Skeleton h={48} />
        <Skeleton h={48} />
      </Stack>
    );
  }
  if (data.content.length === 0) {
    return <StateView kind="empty" compact title="변경 이력 없음" description={emptyMessage} />;
  }

  return (
    <Stack gap="xs" data-testid="change-history">
      <Timeline bulletSize={26} lineWidth={2} color="clay">
        {data.content.map((history) => (
          <HistoryItem key={history.id} history={history} />
        ))}
      </Timeline>
      {!data.last && (
        <Text size="xs" c="dimmed">
          최근 {data.content.length}건만 보입니다 (전체 {data.totalElements}건).
        </Text>
      )}
    </Stack>
  );
}

function HistoryItem({ history }: { history: ChangeHistory }) {
  const Icon = ACTION_ICON[history.action];
  const created = history.action === 'CREATED';
  return (
    <Timeline.Item
      bullet={<Icon size={14} />}
      title={
        <Group gap="xs" wrap="nowrap">
          <Text fw={600} size="sm" component="span">
            {history.summary}
          </Text>
          <Badge
            size="xs"
            variant={history.action === 'UPDATED' ? 'light' : 'outline'}
            color="clay"
          >
            {history.actionLabel}
          </Badge>
        </Group>
      }
    >
      <Text size="xs" c="dimmed" mb={6}>
        {history.changedByName} ({history.changedBy}) · {formatDateTime(history.changedAt)}
      </Text>
      {history.changes.length > 0 && (
        <Table withRowBorders={false} verticalSpacing={2} horizontalSpacing="xs" fz="xs">
          <Table.Tbody>
            {history.changes.map((change) => (
              <Table.Tr key={change.field}>
                <Table.Td c="dimmed" w={96} style={{ whiteSpace: 'nowrap' }}>
                  {change.label}
                </Table.Td>
                {!created && (
                  <Table.Td c="dimmed" style={{ textDecoration: 'line-through' }}>
                    {change.before ?? '(없음)'}
                  </Table.Td>
                )}
                <Table.Td fw={500}>{change.after ?? '(없음)'}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Timeline.Item>
  );
}
