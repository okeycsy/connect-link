import {
  Button,
  Card,
  Grid,
  Group,
  Progress,
  RingProgress,
  Select,
  Skeleton,
  Stack,
  Table,
  Tabs,
  Text,
} from '@mantine/core';
import { IconBell, IconPencil } from '@tabler/icons-react';
import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { ConfirmModal } from '../../../shared/ConfirmModal';
import { formatDate, formatDateTime } from '../../../shared/format';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { ChangeHistoryList } from '../../history/components/ChangeHistoryList';
import { TargetResultsCard } from '../components/TargetResultsCard';
import { OverdueBadge, TestPlanStateBadge } from '../components/TestPlanBadges';
import classes from '../components/TestPlanTable.module.css';
import {
  type TestPlanDetail,
  type TestTargetSummary,
  useDeleteTestPlan,
  useTestPlan,
} from '../hooks';

type Tab = 'results' | 'content' | 'history';
const TABS: Tab[] = ['results', 'content', 'history'];

/**
 * 기관 테스트 수행·결과 관리 페이지. 오른쪽 위에 수행률을 자동 집계해 보인다. 결제원 담당자는 기관별 현황에서 기관을 골라 요구사항별 결과를 보고
 * 의견(미니 수행률·특이사항)을 남긴다. 기관 담당자는 자기 기관 결과를 등록한다.
 */
export function TestPlanDetailPage() {
  const { business: slug = '', id } = useParams<{ business: string; id: string }>();
  const planId = Number(id);
  const { data, isError, error } = useTestPlan(Number.isInteger(planId) ? planId : null);

  if (isError) {
    return (
      <Stack gap="md">
        <PageHeader title="기관 테스트" />
        <ErrorState error={error} homeLink={`/${slug}/home`} />
      </Stack>
    );
  }
  if (!data) {
    return (
      <Stack gap="md">
        <PageHeader title="기관 테스트" />
        <Skeleton h={360} />
      </Stack>
    );
  }
  return <TestPlanView slug={slug} plan={data} />;
}

function TestPlanView({ slug, plan }: { slug: string; plan: TestPlanDetail }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const tabParam = searchParams.get('tab') as Tab | null;
  const tab: Tab = tabParam && TABS.includes(tabParam) ? tabParam : 'results';
  const manage = plan.permissions.manage;
  const orgParam = Number(searchParams.get('org'));
  // 결제원 담당자가 고른 기관(없으면 첫 기관), 기관 담당자는 자기 기관.
  const selectedOrg = manage
    ? (plan.targets.find((t) => t.organizationId === orgParam) ?? plan.targets[0])
    : plan.targets[0];
  const [confirmDelete, setConfirmDelete] = useState(false);
  const remove = useDeleteTestPlan(plan.id);

  const setParams = (next: { tab?: Tab; org?: number }) => {
    const params: Record<string, string> = {};
    const nextTab = next.tab ?? tab;
    if (nextTab !== 'results') params.tab = nextTab;
    const org = next.org ?? (manage && orgParam > 0 ? orgParam : undefined);
    if (org) params.org = String(org);
    setSearchParams(params, { replace: true });
  };

  return (
    <Stack gap="md">
      <PageHeader
        title={plan.title}
        description={
          <>
            {plan.businessLabel} · {plan.subBusiness} · 수행 기간 {formatDate(plan.startDate)} ~{' '}
            {formatDate(plan.endDate)} · 담당 {plan.ownerName}
          </>
        }
        status={
          <Group gap={6} wrap="nowrap">
            <TestPlanStateBadge state={plan.state} />
            {plan.overdue && <OverdueBadge />}
          </Group>
        }
        actions={
          <>
            {manage && (
              <>
                <Button
                  variant="light"
                  component={Link}
                  to={`/${slug}/tests/${plan.id}/recipients`}
                  leftSection={<IconBell size={16} stroke={1.8} />}
                >
                  알림 설정
                </Button>
                <Button
                  variant="default"
                  component={Link}
                  to={`/${slug}/tests/${plan.id}/edit`}
                  leftSection={<IconPencil size={16} stroke={1.8} />}
                >
                  고치기
                </Button>
                <Button color="red" variant="light" onClick={() => setConfirmDelete(true)}>
                  삭제
                </Button>
              </>
            )}
            <ProgressSummary plan={plan} />
          </>
        }
      />

      <Tabs
        value={tab}
        onChange={(v) => setParams({ tab: (v as Tab) ?? 'results' })}
        keepMounted={false}
      >
        <Tabs.List mb="md">
          <Tabs.Tab value="results">결과</Tabs.Tab>
          <Tabs.Tab value="content">내용</Tabs.Tab>
          <Tabs.Tab value="history">변경 이력</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="results">
          <Stack gap="md">
            {manage && (
              <TargetTable
                targets={plan.targets}
                selectedId={selectedOrg?.organizationId}
                onSelect={(t) => setParams({ org: t.organizationId })}
              />
            )}
            {selectedOrg ? (
              <TargetResultsCard
                key={selectedOrg.organizationId}
                planId={plan.id}
                organizationId={selectedOrg.organizationId}
              />
            ) : (
              <StateView kind="empty" description="대상 기관이 없습니다." />
            )}
          </Stack>
        </Tabs.Panel>
        <Tabs.Panel value="content">
          <PlanContent slug={slug} plan={plan} />
        </Tabs.Panel>
        <Tabs.Panel value="history">
          <PlanHistory plan={plan} />
        </Tabs.Panel>
      </Tabs>

      <ConfirmModal
        opened={confirmDelete}
        onClose={() => {
          remove.reset();
          setConfirmDelete(false);
        }}
        title="기관 테스트 삭제"
        message="기관 테스트와 기관이 등록한 결과, 결제원 의견, 가이드 파일을 모두 지웁니다. 되돌릴 수 없습니다."
        confirmLabel="삭제하기"
        danger
        pending={remove.isPending}
        error={remove.error}
        onConfirm={() => remove.mutate(undefined, { onSuccess: () => navigate(`/${slug}/tests`) })}
      />
    </Stack>
  );
}

/** 머리 막대 오른쪽 위의 수행률. 결제원 담당자는 전체(대상 기관 × 요구사항 평균), 기관 담당자는 자기 기관 수행률이다. */
function ProgressSummary({ plan }: { plan: TestPlanDetail }) {
  const manage = plan.permissions.manage;
  const completedTargets = plan.targets.filter((t) => t.completed).length;
  const own = manage ? undefined : plan.targets[0];
  return (
    <Group gap="xs" wrap="nowrap" ml="sm" data-testid="plan-progress">
      <RingProgress
        size={64}
        thickness={7}
        roundCaps
        sections={[{ value: plan.progressRate, color: 'clay' }]}
        label={
          <Text ta="center" fw={700} size="sm" className={classes.numeric}>
            {plan.progressRate}%
          </Text>
        }
      />
      <div>
        <Text size="xs" c="dimmed">
          {manage ? '전체 수행률' : '우리 기관 수행률'}
        </Text>
        <Text size="sm" fw={600} className={classes.numeric}>
          {manage
            ? `완료 기관 ${completedTargets}/${plan.targets.length}`
            : `완료 ${own?.completedCount ?? 0}/${plan.items.length}`}
        </Text>
      </div>
    </Group>
  );
}

/** 결제원 담당자의 기관별 현황. 행을 누르면 아래에 그 기관의 요구사항별 결과가 보인다. */
function TargetTable({
  targets,
  selectedId,
  onSelect,
}: {
  targets: TestTargetSummary[];
  selectedId: number | undefined;
  onSelect: (target: TestTargetSummary) => void;
}) {
  return (
    <Card>
      <Text fw={700} size="xl" mb="sm">
        기관별 현황
      </Text>
      <Table verticalSpacing="sm" highlightOnHover data-testid="target-table">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>기관</Table.Th>
            <Table.Th w={180}>수행률</Table.Th>
            <Table.Th w={240}>결과</Table.Th>
            <Table.Th w={130}>확인할 결과</Table.Th>
            <Table.Th w={150}>마지막 등록</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {targets.map((t) => (
            <Table.Tr
              key={t.organizationId}
              className={`${classes.row} ${t.organizationId === selectedId ? classes.selected : ''}`}
              onClick={() => onSelect(t)}
              tabIndex={0}
              role="button"
              aria-label={`${t.organizationName} 결과 보기`}
              aria-pressed={t.organizationId === selectedId}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  onSelect(t);
                }
              }}
            >
              <Table.Td>
                <Group gap="xs" wrap="nowrap">
                  <Text size="md" fw={600}>
                    {t.organizationName}
                  </Text>
                  {t.completed && (
                    <Text size="xs" fw={600} c="sky.7">
                      수행 완료
                    </Text>
                  )}
                </Group>
              </Table.Td>
              <Table.Td>
                <Group gap="xs" wrap="nowrap">
                  <Progress
                    value={t.progressRate}
                    size="sm"
                    radius="xl"
                    color="clay"
                    w={80}
                    aria-label="수행률"
                  />
                  <Text size="sm" fw={600} className={classes.numeric}>
                    {t.progressRate}%
                  </Text>
                  <Text size="xs" c="dimmed" className={classes.numeric}>
                    {t.completedCount}/{t.itemCount}
                  </Text>
                </Group>
              </Table.Td>
              <Table.Td>
                <Text size="sm" className={classes.numeric}>
                  정상 {t.passed} · 오류 {t.failed} · 수행중 {t.inProgress} · 미수행 {t.notStarted}
                  {t.unreported > 0 && ` · 미등록 ${t.unreported}`}
                </Text>
              </Table.Td>
              <Table.Td>
                {t.awaitingReview > 0 ? (
                  <Text size="sm" fw={600} c="amber.7" className={classes.numeric}>
                    {t.awaitingReview}건
                  </Text>
                ) : (
                  <Text size="sm" c="dimmed">
                    없음
                  </Text>
                )}
              </Table.Td>
              <Table.Td>
                <Text
                  size="sm"
                  c={t.lastReportedAt ? undefined : 'dimmed'}
                  className={classes.numeric}
                >
                  {t.lastReportedAt ? formatDateTime(t.lastReportedAt) : '아직 없음'}
                </Text>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Card>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Text size="sm" c="dimmed" fw={600} mb={2}>
        {label}
      </Text>
      {children}
    </div>
  );
}

/** 테스트 내용: 개발 건, 수행 기간, 내용·가이드, 요구사항, 대상 기관과 알림 대상(결제원 담당자만). */
function PlanContent({ slug, plan }: { slug: string; plan: TestPlanDetail }) {
  const manage = plan.permissions.manage;
  return (
    <Grid gutter="md">
      <Grid.Col span={{ base: 12, lg: 7 }}>
        <Card>
          <Stack gap="md">
            <Text fw={700} size="xl">
              테스트 내용
            </Text>
            <Group gap="xl" align="flex-start">
              <Field label="단위업무">
                <Text size="md">{plan.subBusiness}</Text>
              </Field>
              <Field label="수행 기간">
                <Text size="md" className={classes.numeric}>
                  {formatDate(plan.startDate)} ~ {formatDate(plan.endDate)}
                </Text>
              </Field>
              {plan.completedAt && (
                <Field label="수행 완료">
                  <Text size="md" className={classes.numeric}>
                    {formatDateTime(plan.completedAt)}
                  </Text>
                </Field>
              )}
            </Group>
            <Field label="테스트 내용·수행 가이드">
              {plan.description ? (
                <Text size="md" className={classes.content}>
                  {plan.description}
                </Text>
              ) : (
                <Text size="md" c="dimmed">
                  없음
                </Text>
              )}
            </Field>
            <Field label={`요구사항 ${plan.items.length}개`}>
              <Stack gap={6}>
                {plan.items.map((i) => (
                  <Group key={i.id} gap="sm" align="flex-start" wrap="nowrap">
                    <Text fw={700} size="sm" className={classes.numeric}>
                      {i.number}
                    </Text>
                    <div>
                      <Text size="md">{i.title}</Text>
                      {i.description && (
                        <Text size="sm" c="dimmed" className={classes.content}>
                          {i.description}
                        </Text>
                      )}
                    </div>
                  </Group>
                ))}
              </Stack>
            </Field>
          </Stack>
        </Card>
      </Grid.Col>
      <Grid.Col span={{ base: 12, lg: 5 }}>
        <Stack gap="md">
          <Card>
            <Stack gap="sm">
              <Text fw={700} size="xl">
                가이드·참고자료
              </Text>
              <AttachmentPanel
                ownerType="TEST_PLAN"
                ownerId={plan.id}
                editable={manage}
                uploadLabel="파일 올리기"
                emptyMessage="올린 파일이 없습니다."
              />
            </Stack>
          </Card>
          {manage && (
            <Card>
              <Stack gap="md">
                <Field label={`대상 기관 ${plan.targets.length}곳`}>
                  <Text size="md">{plan.targets.map((t) => t.organizationName).join(', ')}</Text>
                </Field>
                <Field label="결과 등록 알림 대상">
                  <Text size="md" data-testid="plan-recipients">
                    {plan.recipients.length > 0 ? plan.recipients.join(', ') : '없음'}
                  </Text>
                  <Button
                    variant="subtle"
                    size="xs"
                    px={0}
                    mt={4}
                    component={Link}
                    to={`/${slug}/tests/${plan.id}/recipients`}
                  >
                    알림 대상 바꾸기
                  </Button>
                </Field>
                <Text size="xs" c="dimmed">
                  {plan.ownerName} 등록 · {formatDateTime(plan.createdAt)}
                </Text>
              </Stack>
            </Card>
          )}
        </Stack>
      </Grid.Col>
    </Grid>
  );
}

/**
 * 변경 이력. 결제원 담당자는 테스트 내용(요구사항·대상 기관·알림 대상)과 기관별 결과 이력을 골라 보고, 기관 담당자는 자기 기관 결과 이력만 본다.
 */
function PlanHistory({ plan }: { plan: TestPlanDetail }) {
  const manage = plan.permissions.manage;
  const options = [
    ...(manage ? [{ value: 'plan', label: '테스트 내용' }] : []),
    ...plan.targets.map((t) => ({
      value: String(t.targetId),
      label: `${t.organizationName} 결과`,
    })),
  ];
  const [value, setValue] = useState<string>(options[0]?.value ?? 'plan');
  return (
    <Card>
      <Stack gap="md">
        {options.length > 1 && (
          <Select
            aria-label="이력 대상"
            w={260}
            allowDeselect={false}
            data={options}
            value={value}
            onChange={(v) => v && setValue(v)}
          />
        )}
        {value === 'plan' ? (
          <ChangeHistoryList targetType="TEST_PLAN" targetId={plan.id} />
        ) : (
          <ChangeHistoryList targetType="TEST_PLAN_TARGET" targetId={Number(value)} />
        )}
      </Stack>
    </Card>
  );
}
