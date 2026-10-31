import { Badge, Card, Group, Pagination, Skeleton, Stack, Table, Text } from '@mantine/core';
import { useState } from 'react';

import { formatDateTime } from '../../../shared/format';
import { useNotificationOutbox } from '../hooks';
import tableClasses from '../../../shared/list/ResponsiveTable.module.css';
import { PageHeader } from '../../../shared/PageHeader';

const PAGE_SIZE = 20;

/** 발송함(데모 도구). 메일·매터모스트로 나갔을 내용을 실제 발송 대신 여기서 확인한다. */
export function NotificationOutboxPage() {
  const [page, setPage] = useState(0);
  const { data, isPending, isError } = useNotificationOutbox(page, PAGE_SIZE);

  return (
    <Stack gap="md">
      <PageHeader
        title="발송함"
        description="시연 환경은 메일과 매터모스트를 실제로 보내지 않고 여기에 기록합니다."
      />

      <Card data-testid="outbox-table">
        {isError && (
          <Text c="red" size="sm">
            발송함을 불러오지 못했습니다.
          </Text>
        )}
        {isPending ? (
          <Stack gap="xs">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} h={36} />
            ))}
          </Stack>
        ) : (
          <Table verticalSpacing="sm" highlightOnHover className={tableClasses.stack}>
            <Table.Thead>
              <Table.Tr>
                <Table.Th w={110}>채널</Table.Th>
                <Table.Th w={220}>받는 사람</Table.Th>
                <Table.Th>제목·내용</Table.Th>
                <Table.Th w={100}>상태</Table.Th>
                <Table.Th w={150}>기록 일시</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {data?.content.map((o) => (
                <Table.Tr key={o.id}>
                  <Table.Td data-label="채널">
                    <Badge color={o.channel === 'EMAIL' ? 'sky' : 'clay'}>{o.channelLabel}</Badge>
                  </Table.Td>
                  <Table.Td data-label="받는 사람">
                    <Text size="sm" ff="monospace">
                      {o.recipient}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Text size="md" fw={600}>
                      {o.subject}
                    </Text>
                    {o.body && (
                      <Text size="sm" c="dimmed" lineClamp={2}>
                        {o.body}
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td data-label="상태">
                    <Badge variant="outline" color={o.status === 'FAILED' ? 'red' : 'gray'}>
                      {o.statusLabel}
                    </Badge>
                    {o.lastError && (
                      <Text size="xs" c="red.6" mt={4} title={o.lastError} lineClamp={3}>
                        {o.attempts}회 시도 · {o.lastError}
                      </Text>
                    )}
                  </Table.Td>
                  <Table.Td data-label="기록 일시">
                    <Text size="sm" c="dimmed">
                      {formatDateTime(o.createdAt)}
                    </Text>
                  </Table.Td>
                </Table.Tr>
              ))}
              {data && data.content.length === 0 && (
                <Table.Tr>
                  <Table.Td colSpan={5}>
                    <Text c="dimmed" ta="center" py="lg">
                      기록된 발송이 없습니다.
                    </Text>
                  </Table.Td>
                </Table.Tr>
              )}
            </Table.Tbody>
          </Table>
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
