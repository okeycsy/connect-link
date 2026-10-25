import {
  Alert,
  Badge,
  Button,
  Card,
  Grid,
  Group,
  Modal,
  Progress,
  SimpleGrid,
  Skeleton,
  Stack,
  Table,
  Text,
  TextInput,
  UnstyledButton,
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import {
  IconArrowLeft,
  IconMailForward,
  IconPencil,
  IconPlus,
  IconTrash,
} from '@tabler/icons-react';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { Link, Navigate, useLocation, useParams, useSearchParams } from 'react-router-dom';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { ConfirmModal } from '../../../shared/ConfirmModal';
import { formatDate, formatDateTime } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import tableClasses from '../../../shared/list/ResponsiveTable.module.css';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import { useCurrentBusiness } from '../../organization/hooks';
import classes from '../components/ContactTable.module.css';
import {
  type BusinessCode,
  type UpdateCampaign,
  type UpdateCampaignOrganization,
  type UpdateCampaignProgressFilter,
  canManageContacts,
  useDeleteUpdateCampaign,
  useSaveUpdateCampaign,
  useSendCampaignReminders,
  useUpdateCampaignProgress,
  useUpdateCampaignSchedule,
  useUpdateCampaigns,
} from '../hooks';
import { CampaignRemindModal } from '../components/CampaignRemindModal';

/** 예전 주소(관리 메뉴 아래 `/{업무}/admin/update-campaigns`)를 연락처 메뉴 아래 새 주소로 보낸다. ?id= 는 그대로 넘긴다. */
export function UpdateCampaignLegacyRedirect() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const { search } = useLocation();
  return <Navigate to={`/${slug}/contacts/update-campaigns${search}`} replace />;
}

const STATUS_COLOR = { UPCOMING: 'gray', OPEN: 'sky', CLOSED: 'gray' } as const;

/**
 * 정기 변경(현행화) 주간 관리. 결제원 담당자가 업무별 주간을 등록하면 그 업무 참가기관 담당자에게 변경 요청 메일이 간다. 주기(기본
 * 6개월)가 돌아오면 서버가 주간을 스스로 등록하고, 미확인 기관에는 간격마다 리마인드 메일을 다시 보낸다(여기서 바로 보낼 수도 있다).
 * 주간을 고르면 전체 진행률과 기관별 현행화(완료·미확인·정보 변경)를 본다. 담당자는 주간과 상관없이 아무 때나 고칠 수 있다.
 */
export function UpdateCampaignPage() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const current = useCurrentBusiness();
  const { data: me } = useMe();
  const manager = canManageContacts(me, slug);
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: campaigns, isPending, isError, error } = useUpdateCampaigns(manager ? slug : '');
  const { data: schedule } = useUpdateCampaignSchedule(manager ? slug : '');
  const [editing, setEditing] = useState<UpdateCampaign | 'new' | null>(null);

  const idParam = Number(searchParams.get('id')) || null;
  const selected =
    campaigns?.find((c) => c.id === idParam) ??
    campaigns?.find((c) => c.status === 'OPEN') ??
    campaigns?.[0];

  if (me && !manager) {
    return (
      <Stack gap="md">
        <PageHeader title="정기 변경 주간" description="연락처 현행화 주간 등록과 진행률" />
        <StateView kind="forbidden" />
      </Stack>
    );
  }

  return (
    <Stack gap="md">
      <PageHeader
        title="정기 변경 주간"
        description={`${current?.label ?? '이 업무'} 담당자 현행화 주간입니다. 등록하면 참가기관 담당자에게 변경 요청 메일을 보냅니다.`}
        actions={
          <>
            <Button
              component={Link}
              to={`/${slug}/contacts`}
              variant="light"
              leftSection={<IconArrowLeft size={16} stroke={1.8} />}
            >
              담당자 목록
            </Button>
            <Button
              leftSection={<IconPlus size={18} stroke={1.8} />}
              onClick={() => setEditing('new')}
            >
              주간 등록
            </Button>
          </>
        }
      />

      {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
      {!isError && (
        <Grid gutter="md">
          <Grid.Col span={{ base: 12, md: 4 }}>
            <Card data-testid="campaign-list">
              <Text fw={700} size="lg" mb={4}>
                주간 목록
              </Text>
              {schedule && (
                <Text size="xs" c="dimmed" mb="sm" data-testid="campaign-schedule">
                  {schedule.cycleMonths}개월 주기
                  {schedule.nextCycleDate
                    ? ` · 다음 요청 ${formatDate(schedule.nextCycleDate)}${schedule.autoCreate ? '(자동 등록)' : ''}`
                    : ' · 첫 주간을 등록하면 다음 요청일이 정해집니다'}
                  {` · 미확인 기관은 ${schedule.reminderIntervalDays}일마다 리마인드`}
                </Text>
              )}
              {isPending && <Skeleton h={120} />}
              {campaigns && campaigns.length === 0 && (
                <Text size="sm" c="dimmed">
                  등록한 주간이 없습니다.
                </Text>
              )}
              <Stack gap={8}>
                {campaigns?.map((c) => (
                  <UnstyledButton
                    key={c.id}
                    onClick={() => setSearchParams({ id: String(c.id) }, { replace: true })}
                    className={classes.campaignRow}
                    data-active={selected?.id === c.id || undefined}
                    aria-label={c.title}
                  >
                    <Group justify="space-between" wrap="nowrap" gap="xs">
                      <Text size="md" fw={600} lineClamp={1}>
                        {c.title}
                      </Text>
                      <Badge
                        color={STATUS_COLOR[c.status]}
                        variant={c.status === 'OPEN' ? 'light' : 'outline'}
                        size="sm"
                      >
                        {c.statusLabel}
                      </Badge>
                    </Group>
                    <Text size="xs" c="dimmed" className={classes.numeric}>
                      {formatDate(c.startDate)} ~ {formatDate(c.endDate)} · 진행률 {c.progressRate}%
                      {c.autoCreated && ' · 자동 등록'}
                    </Text>
                  </UnstyledButton>
                ))}
              </Stack>
            </Card>
          </Grid.Col>
          <Grid.Col span={{ base: 12, md: 8 }}>
            {selected ? (
              <CampaignProgress
                key={selected.id}
                campaign={selected}
                onEdit={() => setEditing(selected)}
                onDeleted={() => setSearchParams({}, { replace: true })}
              />
            ) : (
              !isPending && (
                <Card>
                  <StateView
                    kind="empty"
                    compact
                    title="주간을 등록해 주세요"
                    description="정기 변경 주간을 등록하면 기관별 현행화 현황이 여기 보입니다."
                  />
                </Card>
              )
            )}
          </Grid.Col>
        </Grid>
      )}

      {current && (
        <CampaignFormModal
          business={current.code}
          campaign={editing === 'new' ? null : editing}
          opened={editing != null}
          onClose={() => setEditing(null)}
          onSaved={(saved) => setSearchParams({ id: String(saved.id) }, { replace: true })}
        />
      )}
    </Stack>
  );
}

const FILTER_OPTIONS: { value: UpdateCampaignProgressFilter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'INCOMPLETE', label: '미확인' },
  { value: 'CHANGED', label: '정보 변경' },
];

function CampaignProgress({
  campaign,
  onEdit,
  onDeleted,
}: {
  campaign: UpdateCampaign;
  onEdit: () => void;
  onDeleted: () => void;
}) {
  const [filter, setFilter] = useState<UpdateCampaignProgressFilter>('ALL');
  const { data, isError, error, isFetching } = useUpdateCampaignProgress(campaign.id, filter);
  const remove = useDeleteUpdateCampaign();
  const [deleting, setDeleting] = useState(false);
  const remind = useSendCampaignReminders();
  // 리마인드 확인 창: 'all' 은 미확인 기관 전부, 기관을 고르면 그 기관 하나.
  const [reminding, setReminding] = useState<'all' | UpdateCampaignOrganization | null>(null);
  const summary = data?.campaign ?? campaign;
  const incomplete = summary.targetCount - summary.completedCount;

  return (
    <Card data-testid="campaign-progress">
      <Group justify="space-between" align="flex-start" mb="md" gap="sm">
        <div>
          <Group gap="xs">
            <Text fw={700} size="xl">
              {summary.title}
            </Text>
            <Badge
              color={STATUS_COLOR[summary.status]}
              variant={summary.status === 'OPEN' ? 'light' : 'outline'}
            >
              {summary.statusLabel}
            </Badge>
          </Group>
          <Text size="sm" c="dimmed" mt={4}>
            {formatDate(summary.startDate)} ~ {formatDate(summary.endDate)} · 등록{' '}
            {summary.registrantName}
          </Text>
        </div>
        <Group gap="xs" wrap="nowrap">
          {summary.status === 'OPEN' && incomplete > 0 && (
            <Button
              variant="light"
              size="xs"
              leftSection={<IconMailForward size={14} stroke={1.8} />}
              onClick={() => {
                remind.reset();
                setReminding('all');
              }}
            >
              미확인 기관 리마인드
            </Button>
          )}
          {summary.status !== 'CLOSED' && (
            <Button
              variant="light"
              size="xs"
              leftSection={<IconPencil size={14} stroke={1.8} />}
              onClick={onEdit}
            >
              고치기
            </Button>
          )}
          {summary.status === 'UPCOMING' && (
            <Button
              variant="light"
              color="red"
              size="xs"
              leftSection={<IconTrash size={14} stroke={1.8} />}
              onClick={() => {
                remove.reset();
                setDeleting(true);
              }}
            >
              삭제
            </Button>
          )}
        </Group>
      </Group>

      <Stack gap={6} mb="md">
        <Group justify="space-between">
          <Text size="sm" fw={600}>
            전체 진행률
          </Text>
          <Text
            size="xl"
            fw={700}
            c="clay.6"
            className={classes.numeric}
            data-testid="campaign-rate"
          >
            {summary.progressRate}%
          </Text>
        </Group>
        <Progress value={summary.progressRate} color="clay" size="md" aria-label="전체 진행률" />
      </Stack>
      <SimpleGrid cols={3} spacing="sm" mb="md">
        <Kpi label="완료 기관" value={summary.completedCount} total={summary.targetCount} />
        <Kpi label="미확인 기관" value={incomplete} total={summary.targetCount} warn />
        <Kpi label="정보 변경 기관" value={summary.changedCount} total={summary.targetCount} />
      </SimpleGrid>

      <Group justify="space-between" mb="xs">
        <ChipFilter<UpdateCampaignProgressFilter>
          label="기관"
          value={filter}
          onChange={setFilter}
          options={FILTER_OPTIONS}
        />
      </Group>
      {isError && <ErrorState error={error} />}
      <div style={{ opacity: isFetching ? 0.6 : 1, transition: 'opacity 120ms' }}>
        <Table
          verticalSpacing="sm"
          data-testid="campaign-organizations"
          className={tableClasses.stack}
        >
          <Table.Thead>
            <Table.Tr>
              <Table.Th>기관</Table.Th>
              <Table.Th w={80}>담당자</Table.Th>
              <Table.Th w={170}>현행화</Table.Th>
              <Table.Th w={150}>정보 변경</Table.Th>
              <Table.Th w={150}>리마인드</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {data?.organizations.map((o) => (
              <Table.Tr key={o.organizationId}>
                <Table.Td>
                  <Text size="md" fw={600}>
                    {o.organizationName}
                  </Text>
                  <Text size="xs" c="dimmed" ff="monospace">
                    {o.organizationCode}
                  </Text>
                </Table.Td>
                <Table.Td data-label="담당자">
                  <Text size="sm" className={classes.numeric}>
                    {o.contactCount}명
                  </Text>
                </Table.Td>
                <Table.Td data-label="현행화">
                  {o.completed ? (
                    <Stack gap={2} align="flex-start">
                      <Badge color="sky" size="sm">
                        완료
                      </Badge>
                      <Text size="xs" c="dimmed" className={classes.numeric}>
                        {formatDateTime(o.completedAt)} {o.completedByName}
                      </Text>
                    </Stack>
                  ) : (
                    <Badge color="amber" size="sm">
                      미확인
                    </Badge>
                  )}
                </Table.Td>
                <Table.Td data-label="정보 변경">
                  {o.changed ? (
                    <Stack gap={2} align="flex-start">
                      <Badge color="clay" size="sm">
                        정보 변경
                      </Badge>
                      <Text size="xs" c="dimmed" className={classes.numeric}>
                        {formatDateTime(o.changedAt)}
                      </Text>
                    </Stack>
                  ) : (
                    <Text size="sm" c="dimmed">
                      -
                    </Text>
                  )}
                </Table.Td>
                <Table.Td data-label="리마인드">
                  <Stack gap={2} align="flex-start">
                    {o.reminderCount > 0 ? (
                      <Text size="xs" c="dimmed" className={classes.numeric}>
                        {o.reminderCount}회 · 최근 {formatDate(o.lastRemindedAt)}
                      </Text>
                    ) : (
                      <Text size="sm" c="dimmed">
                        -
                      </Text>
                    )}
                    {summary.status === 'OPEN' && !o.completed && (
                      <Button
                        variant="subtle"
                        size="compact-xs"
                        aria-label={`${o.organizationName} 리마인드`}
                        onClick={() => {
                          remind.reset();
                          setReminding(o);
                        }}
                      >
                        보내기
                      </Button>
                    )}
                  </Stack>
                </Table.Td>
              </Table.Tr>
            ))}
            {data && data.organizations.length === 0 && (
              <TableEmpty colSpan={5} message="조건에 맞는 기관이 없습니다." />
            )}
          </Table.Tbody>
        </Table>
      </div>

      {reminding != null && (
        <CampaignRemindModal
          opened
          onClose={() => setReminding(null)}
          campaignId={campaign.id}
          organizationIds={reminding === 'all' ? [] : [reminding.organizationId]}
          message={
            reminding === 'all'
              ? `아직 현행화 완료를 누르지 않은 ${incomplete}개 기관의 담당자에게 리마인드 메일을 보냅니다.`
              : `${reminding.organizationName} 담당자에게 리마인드 메일을 보냅니다.`
          }
          pending={remind.isPending}
          error={remind.error}
          onSend={(draft) =>
            remind.mutate(
              {
                id: campaign.id,
                organizationIds: reminding === 'all' ? undefined : [reminding.organizationId],
                ...draft,
              },
              {
                onSuccess: (result) => {
                  setReminding(null);
                  notifications.show({
                    message:
                      `${result.organizationCount}개 기관 ${result.recipientCount}명에게 리마인드 메일을 보냈습니다.` +
                      (result.skippedOrganizationCount > 0
                        ? ` 받을 사람이 없는 기관 ${result.skippedOrganizationCount}곳은 건너뛰었습니다.`
                        : ''),
                    color: 'clay',
                  });
                },
              },
            )
          }
        />
      )}

      <ConfirmModal
        opened={deleting}
        onClose={() => setDeleting(false)}
        title="정기 변경 주간 삭제"
        message={`${summary.title} 주간을 지웁니다. 시작 전인 주간만 지울 수 있습니다.`}
        confirmLabel="삭제"
        danger
        pending={remove.isPending}
        error={remove.error}
        onConfirm={() =>
          remove.mutate(campaign.id, {
            onSuccess: () => {
              setDeleting(false);
              notifications.show({ message: '주간을 지웠습니다.', color: 'clay' });
              onDeleted();
            },
          })
        }
      />
    </Card>
  );
}

function Kpi({
  label,
  value,
  total,
  warn = false,
}: {
  label: string;
  value: number;
  total: number;
  warn?: boolean;
}) {
  return (
    <Card bg="var(--cl-sunken)" withBorder={false} padding="sm" radius="lg">
      <Text size="xs" c="dimmed" fw={600}>
        {label}
      </Text>
      <Text
        size="xl"
        fw={700}
        c={warn && value > 0 ? 'amber.7' : undefined}
        className={classes.numeric}
      >
        {value}
        <Text component="span" size="sm" c="dimmed" fw={500}>
          {' '}
          / {total}
        </Text>
      </Text>
    </Card>
  );
}

interface FormValues {
  title: string;
  startDate: string | null;
  endDate: string | null;
}

function CampaignFormModal({
  business,
  campaign,
  opened,
  onClose,
  onSaved,
}: {
  business: BusinessCode;
  campaign: UpdateCampaign | null;
  opened: boolean;
  onClose: () => void;
  onSaved: (saved: UpdateCampaign) => void;
}) {
  const save = useSaveUpdateCampaign();
  const form = useForm<FormValues>({
    initialValues: { title: '', startDate: null, endDate: null },
  });

  useEffect(() => {
    if (!opened) return;
    form.setValues(
      campaign
        ? { title: campaign.title, startDate: campaign.startDate, endDate: campaign.endDate }
        : { title: '', startDate: null, endDate: null },
    );
    form.clearErrors();
    save.reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened, campaign?.id]);

  const submit = form.onSubmit(async (values) => {
    try {
      const saved = await save.mutateAsync({
        id: campaign?.id,
        body: {
          business,
          title: values.title.trim(),
          startDate: values.startDate ? dayjs(values.startDate).format('YYYY-MM-DD') : '',
          endDate: values.endDate ? dayjs(values.endDate).format('YYYY-MM-DD') : '',
        },
      });
      notifications.show({
        message: campaign
          ? '주간을 고쳤습니다.'
          : '주간을 등록했습니다. 참가기관 담당자에게 메일 알림을 보냈습니다.',
        color: 'clay',
      });
      onSaved(saved);
      onClose();
    } catch (error) {
      const fieldErrors = fieldErrorsOf(error);
      if (Object.keys(fieldErrors).length > 0) form.setErrors(fieldErrors);
    }
  });
  const generalError =
    save.error && Object.keys(fieldErrorsOf(save.error)).length === 0 ? save.error : null;

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={campaign ? '정기 변경 주간 고치기' : '정기 변경 주간 등록'}
      centered
    >
      <form onSubmit={submit} noValidate>
        <Stack gap="md">
          <TextInput
            label="이름"
            placeholder="예: 하반기 담당자 정기 변경"
            required
            {...form.getInputProps('title')}
          />
          <Group grow align="flex-start">
            <DatePickerInput
              label="시작일"
              required
              valueFormat="YYYY-MM-DD"
              {...form.getInputProps('startDate')}
            />
            <DatePickerInput
              label="종료일"
              required
              valueFormat="YYYY-MM-DD"
              {...form.getInputProps('endDate')}
            />
          </Group>
          {generalError && (
            <Alert variant="light" color="red">
              {generalError instanceof ApiError ? generalError.message : '저장하지 못했습니다.'}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={onClose} disabled={save.isPending}>
              닫기
            </Button>
            <Button type="submit" loading={save.isPending}>
              {campaign ? '저장' : '등록'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
