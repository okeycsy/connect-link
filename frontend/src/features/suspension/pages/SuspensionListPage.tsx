import {
  Button,
  Card,
  Group,
  Modal,
  Pagination,
  Select,
  Skeleton,
  Stack,
  Table,
  Text,
} from '@mantine/core';
import { IconPlus, IconSend } from '@tabler/icons-react';
import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { ConfirmModal } from '../../../shared/ConfirmModal';
import { formatDateTime, formatDateTimeRange } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import tableClasses from '../../../shared/list/ResponsiveTable.module.css';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { PageHeader } from '../../../shared/PageHeader';
import { OrgLogo } from '../../../shared/OrgLogo';
import { ErrorState } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import { SuspensionPeriodBadge, SuspensionStatusBadge } from '../components/SuspensionBadges';
import classes from '../components/SuspensionTable.module.css';
import {
  type SuspensionDigest,
  type SuspensionPeriod,
  type SuspensionSchedule,
  type SuspensionSummary,
  canManageSuspensions,
  canRegisterSuspensions,
  useSendSuspensionDigest,
  useSuspensionCounts,
  useOtherOrganizationSchedules,
  useSuspensionOrganizations,
  useSuspensions,
} from '../hooks';

const PAGE_SIZE = 20;
const PERIODS: SuspensionPeriod[] = ['CURRENT', 'UPCOMING', 'PAST'];

/**
 * 일시중단 목록. 결제원 담당자는 이 업무 전 기관의 진행 중·예정·지난 일시중단을 보고, 기관을 골라 기관별 과거 이력을 본다. 기관
 * 담당자는 자기 기관 것만 본다. 행을 누르면 상세로 간다.
 */
export function SuspensionListPage() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const current = useCurrentBusiness();
  const navigate = useNavigate();
  const { data: me } = useMe();
  const manager = canManageSuspensions(me, slug);
  const register = canRegisterSuspensions(me);
  const [searchParams, setSearchParams] = useSearchParams();
  const periodParam = searchParams.get('period') as SuspensionPeriod | null;
  const period: SuspensionPeriod =
    periodParam && PERIODS.includes(periodParam) ? periodParam : 'CURRENT';
  const orgParam = Number(searchParams.get('org'));
  const organizationId = manager && orgParam > 0 ? orgParam : undefined;
  const [page, setPage] = useState(0);
  const [confirmDigest, setConfirmDigest] = useState(false);
  const [digest, setDigest] = useState<SuspensionDigest | null>(null);
  const sendDigest = useSendSuspensionDigest();

  const { data, isPending, isError, error, isFetching } = useSuspensions({
    business: slug,
    period,
    organizationId,
    page,
    size: PAGE_SIZE,
  });
  const { data: counts } = useSuspensionCounts(slug, organizationId);
  const organizations = useSuspensionOrganizations(manager ? slug : '');
  const otherSchedules = useOtherOrganizationSchedules(manager ? '' : slug);

  const setParams = (next: { period?: SuspensionPeriod; org?: number | null }) => {
    const params: Record<string, string> = { period: next.period ?? period };
    const org = next.org === undefined ? organizationId : next.org;
    if (org) params.org = String(org);
    setSearchParams(params, { replace: true });
    setPage(0);
  };

  const countLabel = (label: string, n: number | undefined) =>
    n === undefined ? label : `${label} ${n}`;
  const options = [
    { value: 'CURRENT' as const, label: countLabel('진행 중', counts?.current) },
    { value: 'UPCOMING' as const, label: countLabel('예정', counts?.upcoming) },
    { value: 'PAST' as const, label: countLabel('지난', counts?.past) },
  ];

  return (
    <Stack gap="md">
      <PageHeader
        title="일시중단"
        description={
          manager
            ? `${current?.label ?? '이 업무'} 참가기관의 일시중단입니다. 확인을 시작하고 조치 및 인수인계 사항을 써서 확정합니다.`
            : `우리 기관의 ${current?.label ?? '이 업무'} 일시중단입니다. 등록하면 결제원 담당자에게 메일과 매터모스트 알림이 갑니다.`
        }
        actions={
          <>
            {manager && (
              <Button
                variant="light"
                leftSection={<IconSend size={16} stroke={1.8} />}
                onClick={() => {
                  sendDigest.reset();
                  setConfirmDigest(true);
                }}
              >
                수동으로 보내기
              </Button>
            )}
            {register && (
              <Button
                component={Link}
                to={`/${slug}/suspensions/new`}
                leftSection={<IconPlus size={18} stroke={1.8} />}
              >
                일시중단 등록
              </Button>
            )}
          </>
        }
      />

      <Card>
        <Group justify="space-between" align="center" mb="sm" gap="sm">
          <Group gap="lg" align="center">
            <ChipFilter<SuspensionPeriod>
              label="시기"
              value={period}
              onChange={(v) => setParams({ period: v })}
              options={options}
            />
            {manager && (
              <Select
                aria-label="기관"
                placeholder="전체 기관"
                size="xs"
                w={180}
                clearable
                searchable
                data={(organizations.data ?? []).map((o) => ({
                  value: String(o.id),
                  label: o.name,
                }))}
                value={organizationId ? String(organizationId) : null}
                onChange={(v) => setParams({ org: v ? Number(v) : null })}
              />
            )}
          </Group>
          <Group gap="sm">
            {manager && counts && counts.pendingReview > 0 && (
              <Text size="sm" c="amber.7" fw={600} data-testid="pending-review-count">
                확정 전 {counts.pendingReview}건
              </Text>
            )}
            {data && (
              <Text size="sm" c="dimmed" data-testid="suspension-count">
                {data.totalElements}건
              </Text>
            )}
          </Group>
        </Group>

        {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
        {!isError && (
          <div style={{ opacity: isFetching && data ? 0.6 : 1, transition: 'opacity 120ms' }}>
            <SuspensionTable
              rows={data?.content}
              loading={isPending}
              period={period}
              onSelect={(r) => navigate(`/${slug}/suspensions/${r.id}`)}
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

      {!manager && (
        <Card data-testid="other-schedules">
          <Stack gap={2} mb="sm">
            <Text fw={700}>다른 기관 일정</Text>
            <Text size="sm" c="dimmed">
              {current?.label ?? '이 업무'}에서 결제원이 확정한 다른 기관의 진행 중·예정
              일시중단입니다. 일정만 보이고 자세한 내용은 그 기관과 결제원만 봅니다.
            </Text>
          </Stack>
          {otherSchedules.isError ? (
            <ErrorState error={otherSchedules.error} />
          ) : (
            <ScheduleTable rows={otherSchedules.data} />
          )}
        </Card>
      )}

      <ConfirmModal
        opened={confirmDigest}
        onClose={() => setConfirmDigest(false)}
        title="전 영업일 취합 수동으로 보내기"
        message="오늘까지 확정된 일시중단 중 다음 날부터 다음 영업일까지 시작하는 건을 운영·관제 매터모스트 채널로 지금 보냅니다. 오늘 오후 4시 자동 발송은 건너뜁니다."
        confirmLabel="보내기"
        pending={sendDigest.isPending}
        error={sendDigest.error}
        onConfirm={() =>
          sendDigest.mutate(undefined, {
            onSuccess: (d) => {
              setConfirmDigest(false);
              setDigest(d);
            },
          })
        }
      />
      <Modal
        opened={digest != null}
        onClose={() => setDigest(null)}
        title="전 영업일 취합을 보냈습니다"
        centered
        size="lg"
      >
        {digest && (
          <Stack gap="sm">
            <Text size="sm" c="dimmed">
              {digest.recipients.join(', ')} · {formatDateTime(digest.sentAt)} · 확정{' '}
              {digest.itemCount}건
            </Text>
            <Card bg="var(--cl-sunken)" data-testid="digest-body">
              <Text size="sm" className={classes.content}>
                {digest.body}
              </Text>
            </Card>
          </Stack>
        )}
      </Modal>
    </Stack>
  );
}

const EMPTY_MESSAGE: Record<SuspensionPeriod, string> = {
  CURRENT: '지금 중단 중인 기관이 없습니다.',
  UPCOMING: '예정된 일시중단이 없습니다.',
  PAST: '지난 일시중단이 없습니다.',
};

function SuspensionTable({
  rows,
  loading,
  period,
  onSelect,
}: {
  rows: SuspensionSummary[] | undefined;
  loading: boolean;
  period: SuspensionPeriod;
  onSelect: (row: SuspensionSummary) => void;
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
      data-testid="suspension-table"
      className={tableClasses.stack}
    >
      <Table.Thead>
        <Table.Tr>
          <Table.Th w={150}>기관</Table.Th>
          <Table.Th w={290}>중단 일시</Table.Th>
          <Table.Th>중단 이유</Table.Th>
          <Table.Th w={110}>상태</Table.Th>
          <Table.Th w={110}>등록</Table.Th>
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
            aria-label={`${r.organizationName} ${formatDateTimeRange(r.startAt, r.endAt)}`}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelect(r);
              }
            }}
          >
            <Table.Td>
              <Group gap={8} wrap="nowrap">
                <OrgLogo code={r.organizationCode} />
                <Text size="md" fw={600}>
                  {r.organizationName}
                </Text>
              </Group>
              <Text size="xs" c="dimmed" lineClamp={1}>
                {r.targetWorks.map((w) => w.name).join(', ')}
              </Text>
            </Table.Td>
            <Table.Td data-label="중단 일시">
              <Group gap={6} wrap="nowrap">
                <Text size="sm" className={classes.when}>
                  {formatDateTimeRange(r.startAt, r.endAt)}
                </Text>
                <SuspensionPeriodBadge period={r.period} size="xs" />
              </Group>
            </Table.Td>
            <Table.Td data-label="중단 이유">
              <Text size="sm" lineClamp={1}>
                {r.reason}
              </Text>
            </Table.Td>
            <Table.Td data-label="상태">
              <SuspensionStatusBadge status={r.status} size="xs" />
            </Table.Td>
            <Table.Td data-label="등록">
              <Text size="sm">
                {r.registrantName}
                {r.registrantJobTitle && ` ${r.registrantJobTitle}`}
              </Text>
              <Text size="xs" c="dimmed">
                {r.registrantKftc ? '결제원' : '기관'}
              </Text>
            </Table.Td>
          </Table.Tr>
        ))}
        {rows && rows.length === 0 && <TableEmpty colSpan={5} message={EMPTY_MESSAGE[period]} />}
      </Table.Tbody>
    </Table>
  );
}

/** 다른 기관 일정. 행은 열리지 않는다(일정만 공유). */
function ScheduleTable({ rows }: { rows: SuspensionSchedule[] | undefined }) {
  if (!rows) {
    return (
      <Stack gap="xs">
        {[0, 1].map((i) => (
          <Skeleton key={i} h={44} />
        ))}
      </Stack>
    );
  }
  return (
    <Table verticalSpacing="sm" className={tableClasses.stack}>
      <Table.Thead>
        <Table.Tr>
          <Table.Th w={180}>기관</Table.Th>
          <Table.Th w={290}>중단 일시</Table.Th>
          <Table.Th>대상 업무</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {rows.map((r, i) => (
          <Table.Tr key={`${r.organizationCode}-${r.startAt}-${i}`}>
            <Table.Td>
              <Group gap={8} wrap="nowrap">
                <OrgLogo code={r.organizationCode} />
                <Text size="md" fw={600}>
                  {r.organizationName}
                </Text>
              </Group>
            </Table.Td>
            <Table.Td data-label="중단 일시">
              <Group gap={6} wrap="nowrap">
                <Text size="sm" className={classes.when}>
                  {formatDateTimeRange(r.startAt, r.endAt)}
                </Text>
                <SuspensionPeriodBadge period={r.period} size="xs" />
              </Group>
            </Table.Td>
            <Table.Td data-label="대상 업무">
              <Text size="sm">{r.targetWorkNames}</Text>
            </Table.Td>
          </Table.Tr>
        ))}
        {rows.length === 0 && (
          <TableEmpty colSpan={3} message="확정된 다른 기관 일정이 없습니다." />
        )}
      </Table.Tbody>
    </Table>
  );
}
