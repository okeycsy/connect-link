import {
  Button,
  Card,
  Group,
  Pagination,
  Progress,
  Skeleton,
  Stack,
  Table,
  Text,
} from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { formatDate } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import { DueBadge, OpinionStatusBadge, ReplyStatusBadge } from '../components/OpinionBadges';
import classes from '../components/OpinionTable.module.css';
import {
  type OpinionRequestStatus,
  type OpinionRequestSummary,
  canManageOpinions,
  useOpinionRequestCounts,
  useOpinionRequests,
} from '../hooks';

const PAGE_SIZE = 20;

const STATUSES: OpinionRequestStatus[] = ['DRAFT', 'OPEN', 'CLOSED'];

/**
 * 의견 요청 목록. 결제원 담당자는 이 업무의 요청을 상태별로 보고 회신율을 함께 본다. 기관 담당자는 자기 기관이 대상인 회신중·마감
 * 요청과 내 기관 회신 상태를 본다. 행을 누르면 상세로 간다.
 */
export function OpinionListPage() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const current = useCurrentBusiness();
  const navigate = useNavigate();
  const { data: me } = useMe();
  const manager = canManageOpinions(me, slug);
  const [searchParams, setSearchParams] = useSearchParams();
  const statusParam = searchParams.get('status') as OpinionRequestStatus | null;
  const status: OpinionRequestStatus =
    statusParam && STATUSES.includes(statusParam) ? statusParam : 'OPEN';
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error, isFetching } = useOpinionRequests({
    business: slug,
    status,
    page,
    size: PAGE_SIZE,
  });
  const { data: counts } = useOpinionRequestCounts(slug);
  const countLabel = (label: string, n: number | undefined) =>
    n === undefined ? label : `${label} ${n}`;

  const options = [
    ...(manager ? [{ value: 'DRAFT' as const, label: countLabel('작성중', counts?.draft) }] : []),
    { value: 'OPEN' as const, label: countLabel('회신중', counts?.open) },
    { value: 'CLOSED' as const, label: countLabel('마감', counts?.closed) },
  ];

  return (
    <Stack gap="md">
      <PageHeader
        title="의견 요청"
        description={
          manager
            ? `${current?.label ?? '이 업무'} 참가기관에 의견을 묻고 회신을 모읍니다. 회신 현황과 취합표는 요청을 눌러 봅니다.`
            : '우리 기관에 온 의견 요청입니다. 요청을 눌러 의견을 쓰고 제출합니다.'
        }
        actions={
          manager && (
            <Button
              component={Link}
              to={`/${slug}/opinions/new`}
              leftSection={<IconPlus size={18} stroke={1.8} />}
            >
              새 의견 요청
            </Button>
          )
        }
      />

      <Card>
        <Group justify="space-between" align="center" mb="sm" gap="sm">
          <ChipFilter<OpinionRequestStatus>
            label="상태"
            value={status}
            onChange={(v) => {
              setSearchParams({ status: v }, { replace: true });
              setPage(0);
            }}
            options={options}
          />
          {data && (
            <Text size="sm" c="dimmed" data-testid="opinion-count">
              {data.totalElements}건
            </Text>
          )}
        </Group>

        {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
        {!isError && (
          <div style={{ opacity: isFetching && data ? 0.6 : 1, transition: 'opacity 120ms' }}>
            <OpinionRequestTable
              rows={data?.content}
              loading={isPending}
              manager={manager}
              status={status}
              onSelect={(r) => navigate(`/${slug}/opinions/${r.id}`)}
            />
          </div>
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

const EMPTY_MESSAGE: Record<OpinionRequestStatus, string> = {
  DRAFT: '작성 중인 의견 요청이 없습니다.',
  OPEN: '회신을 받고 있는 의견 요청이 없습니다.',
  CLOSED: '마감한 의견 요청이 없습니다.',
};

function OpinionRequestTable({
  rows,
  loading,
  manager,
  status,
  onSelect,
}: {
  rows: OpinionRequestSummary[] | undefined;
  loading: boolean;
  manager: boolean;
  status: OpinionRequestStatus;
  onSelect: (row: OpinionRequestSummary) => void;
}) {
  if (loading && !rows) {
    return (
      <Stack gap="xs">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} h={44} />
        ))}
      </Stack>
    );
  }
  return (
    <Table verticalSpacing="sm" highlightOnHover data-testid="opinion-table">
      <Table.Thead>
        <Table.Tr>
          <Table.Th>요청명</Table.Th>
          <Table.Th w={150}>회신 기한</Table.Th>
          {manager ? (
            <Table.Th w={200}>회신</Table.Th>
          ) : (
            <Table.Th w={140}>우리 기관 회신</Table.Th>
          )}
          <Table.Th w={120}>담당</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rows?.map((r) => {
          const target = r.targetCount ?? 0;
          const replied = r.repliedCount ?? 0;
          return (
            <Table.Tr
              key={r.id}
              className={classes.row}
              onClick={() => onSelect(r)}
              tabIndex={0}
              role="button"
              aria-label={r.title}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onSelect(r);
                }
              }}
            >
              <Table.Td>
                <Group gap="xs" wrap="nowrap">
                  <Text size="md" fw={600}>
                    {r.title}
                  </Text>
                  {r.cancelled && <OpinionStatusBadge status={r.status} cancelled size="xs" />}
                </Group>
              </Table.Td>
              <Table.Td>
                <Group gap={6} wrap="nowrap">
                  <Text size="sm" className={classes.numeric}>
                    {formatDate(r.dueDate)}
                  </Text>
                  <DueBadge status={r.status} daysLeft={r.daysLeft} size="xs" />
                </Group>
              </Table.Td>
              <Table.Td>
                {manager ? (
                  status === 'DRAFT' ? (
                    <Text size="sm" c="dimmed">
                      대상 {target}곳
                    </Text>
                  ) : (
                    <Group gap="xs" wrap="nowrap">
                      <Progress
                        value={target === 0 ? 0 : (replied * 100) / target}
                        size="sm"
                        radius="xl"
                        color="clay"
                        w={80}
                        aria-label="회신율"
                      />
                      <Text size="sm" className={classes.numeric}>
                        {replied}/{target}곳
                      </Text>
                    </Group>
                  )
                ) : (
                  <ReplyStatusBadge status={r.myReplyStatus} size="xs" />
                )}
              </Table.Td>
              <Table.Td>
                <Text size="sm">{r.ownerName}</Text>
              </Table.Td>
            </Table.Tr>
          );
        })}
        {rows && rows.length === 0 && <TableEmpty colSpan={4} message={EMPTY_MESSAGE[status]} />}
      </Table.Tbody>
    </Table>
  );
}
