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

import { formatDate, formatDateTime } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import tableClasses from '../../../shared/list/ResponsiveTable.module.css';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import { OverdueBadge, TestPlanStateBadge } from '../components/TestPlanBadges';
import classes from '../components/TestPlanTable.module.css';
import {
  type TestPlanState,
  type TestPlanSummary,
  canManageTestPlans,
  useTestPlanCounts,
  useTestPlans,
} from '../hooks';

const PAGE_SIZE = 20;
const STATES: TestPlanState[] = ['IN_PROGRESS', 'COMPLETED'];

/**
 * 기관 테스트 목록. 결제원 담당자는 이 업무의 모든 기관 테스트와 전체 수행률·완료 기관 수를, 기관 담당자는 자기 기관이 대상인 테스트와
 * 자기 기관 수행률을 본다. 진행 중은 끝나는 날이 가까운 순, 수행 완료는 최근 완료 순이다. 행을 누르면 상세로 간다.
 */
export function TestPlanListPage() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const current = useCurrentBusiness();
  const navigate = useNavigate();
  const { data: me } = useMe();
  const manager = canManageTestPlans(me, slug);
  const [searchParams, setSearchParams] = useSearchParams();
  const stateParam = searchParams.get('state') as TestPlanState | null;
  const state: TestPlanState =
    stateParam && STATES.includes(stateParam) ? stateParam : 'IN_PROGRESS';
  const [page, setPage] = useState(0);

  const { data, isPending, isError, error, isFetching } = useTestPlans({
    business: slug,
    state,
    page,
    size: PAGE_SIZE,
  });
  const { data: counts } = useTestPlanCounts(slug);

  const countLabel = (label: string, n: number | undefined) =>
    n === undefined ? label : `${label} ${n}`;
  const options = [
    { value: 'IN_PROGRESS' as const, label: countLabel('진행 중', counts?.inProgress) },
    { value: 'COMPLETED' as const, label: countLabel('수행 완료', counts?.completed) },
  ];

  return (
    <Stack gap="md">
      <PageHeader
        title="기관 테스트"
        description={
          manager
            ? `${current?.label ?? '이 업무'} 단위업무·개발 건별 기관 테스트입니다. 기관이 요구사항별 결과를 등록하면 수행률이 집계되고 알림 대상에게 매터모스트 알림이 갑니다.`
            : `우리 기관이 대상인 ${current?.label ?? '이 업무'} 기관 테스트입니다. 요구사항별 결과를 등록해 주세요.`
        }
        actions={
          manager && (
            <Button
              component={Link}
              to={`/${slug}/tests/new`}
              leftSection={<IconPlus size={18} stroke={1.8} />}
            >
              기관 테스트 만들기
            </Button>
          )
        }
      />

      <Card>
        <Group justify="space-between" align="center" mb="sm" gap="sm">
          <ChipFilter<TestPlanState>
            label="진행"
            value={state}
            onChange={(v) => {
              setSearchParams({ state: v }, { replace: true });
              setPage(0);
            }}
            options={options}
          />
          {data && (
            <Text size="sm" c="dimmed" data-testid="test-plan-count">
              {data.totalElements}건
            </Text>
          )}
        </Group>

        {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
        {!isError && (
          <div style={{ opacity: isFetching && data ? 0.6 : 1, transition: 'opacity 120ms' }}>
            <TestPlanTable
              rows={data?.content}
              loading={isPending}
              manager={manager}
              state={state}
              onSelect={(r) => navigate(`/${slug}/tests/${r.id}`)}
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

const EMPTY_MESSAGE: Record<TestPlanState, string> = {
  IN_PROGRESS: '진행 중인 기관 테스트가 없습니다.',
  COMPLETED: '수행을 마친 기관 테스트가 없습니다.',
};

function TestPlanTable({
  rows,
  loading,
  manager,
  state,
  onSelect,
}: {
  rows: TestPlanSummary[] | undefined;
  loading: boolean;
  manager: boolean;
  state: TestPlanState;
  onSelect: (row: TestPlanSummary) => void;
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
    <Table
      verticalSpacing="sm"
      highlightOnHover
      data-testid="test-plan-table"
      className={tableClasses.stack}
    >
      <Table.Thead>
        <Table.Tr>
          <Table.Th>개발 건</Table.Th>
          <Table.Th w={190}>수행 기간</Table.Th>
          <Table.Th w={150}>{manager ? '전체 수행률' : '우리 기관 수행률'}</Table.Th>
          <Table.Th w={manager ? 100 : 140}>{manager ? '완료 기관' : '완료 요구사항'}</Table.Th>
          <Table.Th w={150}>상태</Table.Th>
          <Table.Th w={80}>담당</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rows?.map((r) => (
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
              <Text size="md" fw={600} lineClamp={2}>
                {r.title}
              </Text>
              <Text size="xs" c="dimmed" lineClamp={1}>
                {r.subBusiness} · 요구사항 {r.itemCount}개
              </Text>
            </Table.Td>
            <Table.Td data-label="수행 기간">
              <Text size="sm" className={classes.numeric}>
                {formatDate(r.startDate)} ~ {formatDate(r.endDate)}
              </Text>
            </Table.Td>
            <Table.Td data-label="수행률">
              <Group gap="xs" wrap="nowrap">
                <Progress
                  value={r.progressRate}
                  size="sm"
                  radius="xl"
                  color="clay"
                  w={70}
                  aria-label="수행률"
                />
                <Text size="sm" fw={600} className={classes.numeric} data-testid="progress-rate">
                  {r.progressRate}%
                </Text>
              </Group>
            </Table.Td>
            <Table.Td data-label="완료">
              {manager ? (
                <Text size="sm" className={classes.numeric}>
                  {r.completedTargetCount ?? 0}/{r.targetCount ?? 0}곳
                </Text>
              ) : (
                <>
                  <Text size="sm" className={classes.numeric}>
                    {r.ownCompletedCount ?? 0}/{r.itemCount}개
                  </Text>
                  <Text size="xs" c="dimmed">
                    {r.ownLastReportedAt
                      ? `${formatDateTime(r.ownLastReportedAt)} 등록`
                      : '아직 등록 전'}
                  </Text>
                </>
              )}
            </Table.Td>
            <Table.Td data-label="상태">
              <Group gap={6} wrap="nowrap">
                <TestPlanStateBadge state={r.state} size="xs" />
                {r.overdue && <OverdueBadge size="xs" />}
              </Group>
            </Table.Td>
            <Table.Td data-label="담당">
              <Text size="sm">{r.ownerName}</Text>
            </Table.Td>
          </Table.Tr>
        ))}
        {rows && rows.length === 0 && <TableEmpty colSpan={6} message={EMPTY_MESSAGE[state]} />}
      </Table.Tbody>
    </Table>
  );
}
