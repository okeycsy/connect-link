import { Badge, Card, Group, Pagination, Skeleton, Stack, Table, Text, Title } from '@mantine/core';
import { useState } from 'react';

import { formatDateTime } from '../../../shared/format';
import { useNotificationOutbox } from '../hooks';

const PAGE_SIZE = 20;

/** 발송함(데모 도구). 메일·매터모스트로 나갔을 내용을 실제 발송 대신 여기서 확인한다. */
export function NotificationOutboxPage() {
  const [page, setPage] = useState(0);
  const { data, isPending, isError } = useNotificationOutbox(page, PAGE_SIZE);

  return (
    <Stack gap="md">
      <div>
        <Title order={2}>발송함</Title>
        <Text c="dimmed" size="lg">
          시연 환경은 메일과 매터모스트를 실제로 보내지 않고 여기에 기록합니다.
        </Text>
      </div>

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
          <Table verticalSpacing="sm" highlightOnHover>
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
                  <Table.Td>
                    <Badge variant={o.channel === 'EMAIL' ? 'light' : 'outline'} color="navy">
                      {o.channelLabel}
                    </Badge>
                  </Table.Td>
                  <Table.Td>
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
                  <Table.Td>
                    <Badge variant="outline" color="navy">
                      {o.statusLabel}
                    </Badge>
                  </Table.Td>
                  <Table.Td>
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
