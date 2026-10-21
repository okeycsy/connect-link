import { Alert, Button, Card, Group, Skeleton, Stack, Table, Text, Timeline } from '@mantine/core';
import { IconDownload, IconMailForward } from '@tabler/icons-react';

import { errorCodeOf } from '../../../api/client';
import { formatDateTime } from '../../../shared/format';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import {
  type OpinionRequestDetail,
  useDownloadOpinionExcel,
  useOpinionCollation,
  useOpinionReminders,
} from '../hooks';
import classes from './OpinionTable.module.css';

/**
 * 기관별 취합표. 대상 기관마다 한 줄로 질문별 답을 모으고, 엑셀(결과 요약, 질문별 답, 기관별 취합표)로
 * 내려받는다.
 */
export function CollationPanel({ request }: { request: OpinionRequestDetail }) {
  const { data, isPending, error } = useOpinionCollation(request.id);
  const excel = useDownloadOpinionExcel(request.id);

  if (error) return <StateView kind={stateKindOf(error)} compact />;
  if (isPending || !data) return <Skeleton h={320} />;
  const { summary, questions } = data;

  return (
    <Card>
      <Group justify="space-between" mb="sm" gap="sm" align="flex-start">
        <div>
          <Text fw={700} size="xl">
            기관별 취합표
          </Text>
          <Text size="sm" c="dimmed" data-testid="collation-summary">
            대상 {summary.targetCount}곳 · 제출 {summary.repliedCount}곳(회신율 {summary.replyRate}
            %)
          </Text>
        </div>
        <Button
          variant="light"
          leftSection={<IconDownload size={18} stroke={1.8} />}
          loading={excel.isPending}
          onClick={() => excel.mutate()}
        >
          엑셀 다운로드
        </Button>
      </Group>
      {excel.isError && (
        <Alert
          variant="light"
          color={errorCodeOf(excel.error) === 'INTERNAL_ONLY' ? 'amber' : 'red'}
          mb="sm"
          data-testid="excel-error"
        >
          {errorCodeOf(excel.error) === 'INTERNAL_ONLY'
            ? '엑셀 다운로드는 결제원 내부망에서만 됩니다.'
            : excel.error.message}
        </Alert>
      )}
      <Table.ScrollContainer minWidth={400 + questions.length * 180}>
        <Table verticalSpacing="sm" data-testid="collation-table">
          <Table.Thead>
            <Table.Tr>
              <Table.Th w={150}>기관</Table.Th>
              <Table.Th w={90}>회신</Table.Th>
              {questions.map((q, index) => (
                <Table.Th key={q.questionId} w={160}>
                  {index + 1}. {q.title}
                </Table.Th>
              ))}
              <Table.Th w={150}>회신자 · 제출</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {data.rows.map((r) => (
              <Table.Tr key={r.targetId}>
                <Table.Td>
                  <Text fw={600}>{r.organizationName}</Text>
                </Table.Td>
                <Table.Td>
                  {r.replied ? (
                    <Text size="sm">회신</Text>
                  ) : (
                    <Text size="sm" c="dimmed">
                      미회신
                    </Text>
                  )}
                </Table.Td>
                {questions.map((q) => {
                  const answer = r.answers.find((a) => a.questionId === q.questionId);
                  return (
                    <Table.Td key={q.questionId}>
                      {answer?.skipped ? (
                        <Text size="sm" c="dimmed" data-testid="answer-skipped">
                          건너뜀
                        </Text>
                      ) : (
                        <Text size="sm" className={classes.content}>
                          {answer?.value ?? ''}
                        </Text>
                      )}
                    </Table.Td>
                  );
                })}
                <Table.Td>
                  {r.replied && (
                    <Text size="sm" className={classes.numeric}>
                      {r.replierName}
                      <br />
                      {formatDateTime(r.submittedAt)}
                    </Text>
                  )}
                </Table.Td>
              </Table.Tr>
            ))}
            {data.rows.length === 0 && (
              <TableEmpty colSpan={3 + questions.length} message="대상 기관이 없습니다." />
            )}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Card>
  );
}

/** 리마인드 이력. 보낸 일시, 보낸 사람, 받은 기관과 담당자 수. */
export function ReminderHistory({ request }: { request: OpinionRequestDetail }) {
  const { data, isPending, error } = useOpinionReminders(request.id);
  if (error) return <StateView kind={stateKindOf(error)} compact />;
  if (isPending || !data) return <Skeleton h={160} />;
  if (data.length === 0) {
    return (
      <Card>
        <StateView kind="empty" compact description="보낸 리마인드 메일이 없습니다." />
      </Card>
    );
  }
  return (
    <Card>
      <Timeline bulletSize={24} lineWidth={2} color="clay" data-testid="reminder-history">
        {data.map((r) => (
          <Timeline.Item
            key={r.id}
            bullet={<IconMailForward size={14} stroke={1.8} />}
            title={
              <Text fw={600} size="md">
                {r.organizationCount}곳 {r.recipientCount}명에게 리마인드
              </Text>
            }
          >
            <Stack gap={2}>
              <Text size="sm">{r.organizationNames.join(', ')}</Text>
              <Text size="xs" c="dimmed" className={classes.numeric}>
                {r.senderName} · {formatDateTime(r.sentAt)}
              </Text>
            </Stack>
          </Timeline.Item>
        ))}
      </Timeline>
    </Card>
  );
}
