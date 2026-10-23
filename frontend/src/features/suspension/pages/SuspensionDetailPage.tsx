import {
  Alert,
  Button,
  Card,
  Grid,
  Group,
  Modal,
  Skeleton,
  Stack,
  Tabs,
  Text,
  Textarea,
  ThemeIcon,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { IconCheck, IconEye, IconPencil } from '@tabler/icons-react';
import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { fieldErrorsOf } from '../../../api/client';
import { ConfirmModal } from '../../../shared/ConfirmModal';
import {
  formatAuthor,
  formatDate,
  formatDateTime,
  formatDateTimeRange,
  formatPhone,
} from '../../../shared/format';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { ChangeHistoryList } from '../../history/components/ChangeHistoryList';
import { SuspensionPeriodBadge, SuspensionStatusBadge } from '../components/SuspensionBadges';
import classes from '../components/SuspensionTable.module.css';
import {
  type SuspensionDetail,
  useConfirmSuspension,
  useDeleteSuspension,
  useStartSuspensionReview,
  useSuspension,
} from '../hooks';

type Tab = 'content' | 'history';

/**
 * 일시중단 상세. 등록 양식 그대로(일시, 업무, 이유, 결제원 조치사항, 담당자 연락처, 공문, 추가 사항)와 검토·확정 기록을 보인다. 결제원
 * 담당자는 여기서 확인을 시작하고 조치 및 인수인계 사항을 써서 확정한다.
 */
export function SuspensionDetailPage() {
  const { business: slug = '', id } = useParams<{ business: string; id: string }>();
  const suspensionId = Number(id);
  const { data, isError, error } = useSuspension(
    Number.isInteger(suspensionId) ? suspensionId : null,
  );

  if (isError) {
    return (
      <Stack gap="md">
        <PageHeader title="일시중단" />
        <ErrorState error={error} homeLink={`/${slug}/home`} />
      </Stack>
    );
  }
  if (!data) {
    return (
      <Stack gap="md">
        <PageHeader title="일시중단" />
        <Skeleton h={360} />
      </Stack>
    );
  }
  return <SuspensionView slug={slug} suspension={data} />;
}

type Dialog = 'review' | 'confirm' | 'delete' | null;

function SuspensionView({ slug, suspension }: { slug: string; suspension: SuspensionDetail }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab: Tab = searchParams.get('tab') === 'history' ? 'history' : 'content';
  const [dialog, setDialog] = useState<Dialog>(null);
  const p = suspension.permissions;
  const review = useStartSuspensionReview(suspension.id);
  const remove = useDeleteSuspension(suspension.id);
  const closeDialog = () => {
    review.reset();
    remove.reset();
    setDialog(null);
  };
  const confirmed = suspension.status === 'CONFIRMED';

  return (
    <Stack gap="md">
      <PageHeader
        title={`${suspension.organizationName} 일시중단`}
        description={
          <>
            {suspension.targetWorks.map((w) => w.name).join(', ')} · 등록{' '}
            {formatAuthor(
              suspension.registrantKftc ? '결제원' : suspension.organizationName,
              suspension.registrantName,
              suspension.registrantJobTitle,
            )}{' '}
            {formatDate(suspension.createdAt)}
          </>
        }
        status={
          <Group gap={6} wrap="nowrap">
            <SuspensionStatusBadge status={suspension.status} />
            <SuspensionPeriodBadge period={suspension.period} />
          </Group>
        }
        actions={
          <>
            {p.edit && (
              <Button
                variant="default"
                component={Link}
                to={`/${slug}/suspensions/${suspension.id}/edit`}
                leftSection={<IconPencil size={16} stroke={1.8} />}
              >
                고치기
              </Button>
            )}
            {p.delete && (
              <Button color="red" variant="light" onClick={() => setDialog('delete')}>
                삭제
              </Button>
            )}
            {p.startReview && (
              <Button
                variant="light"
                leftSection={<IconEye size={16} stroke={1.8} />}
                onClick={() => setDialog('review')}
              >
                확인 시작
              </Button>
            )}
            {p.confirm && (
              <Button
                leftSection={<IconCheck size={16} stroke={1.8} />}
                onClick={() => setDialog('confirm')}
              >
                {confirmed ? '조치 사항 고치기' : '조치 쓰고 확정'}
              </Button>
            )}
          </>
        }
      />

      <Tabs
        value={tab}
        onChange={(v) =>
          setSearchParams(v === 'history' ? { tab: 'history' } : {}, { replace: true })
        }
        keepMounted={false}
      >
        <Tabs.List mb="md">
          <Tabs.Tab value="content">내용</Tabs.Tab>
          <Tabs.Tab value="history">변경 이력</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="content">
          <Grid gutter="md">
            <Grid.Col span={{ base: 12, lg: 7 }}>
              <SuspensionContent suspension={suspension} />
            </Grid.Col>
            <Grid.Col span={{ base: 12, lg: 5 }}>
              <ReviewCard suspension={suspension} />
            </Grid.Col>
          </Grid>
        </Tabs.Panel>
        <Tabs.Panel value="history">
          <Card>
            <ChangeHistoryList targetType="SUSPENSION" targetId={suspension.id} />
          </Card>
        </Tabs.Panel>
      </Tabs>

      <ConfirmModal
        opened={dialog === 'review'}
        onClose={closeDialog}
        title="확인 시작"
        message="이 일시중단을 확인하기 시작합니다. 확인자로 이름이 남고, 검토 대기였다면 검토 중으로 바뀝니다."
        confirmLabel="확인 시작"
        pending={review.isPending}
        error={review.error}
        onConfirm={() => review.mutate(undefined, { onSuccess: closeDialog })}
      />
      <ConfirmModal
        opened={dialog === 'delete'}
        onClose={closeDialog}
        title="일시중단 삭제"
        message="일시중단과 공문을 지웁니다. 결제원 담당자에게 매터모스트 알림이 갑니다. 되돌릴 수 없습니다."
        confirmLabel="삭제하기"
        danger
        pending={remove.isPending}
        error={remove.error}
        onConfirm={() =>
          remove.mutate(undefined, { onSuccess: () => navigate(`/${slug}/suspensions`) })
        }
      />
      <HandoverModal opened={dialog === 'confirm'} onClose={closeDialog} suspension={suspension} />
    </Stack>
  );
}

/** 이름표 + 값 한 줄. 값이 없으면 "없음". */
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

function Body({ text }: { text: string | null | undefined }) {
  return text ? (
    <Text size="md" className={classes.content}>
      {text}
    </Text>
  ) : (
    <Text size="md" c="dimmed">
      없음
    </Text>
  );
}

/** 등록 양식의 내용. */
function SuspensionContent({ suspension: s }: { suspension: SuspensionDetail }) {
  return (
    <Card>
      <Stack gap="md">
        <Text fw={700} size="xl">
          중단 내용
        </Text>
        <Field label="중단 일시">
          <Text size="lg" fw={600} className={classes.numeric} data-testid="suspension-period">
            {formatDateTimeRange(s.startAt, s.endAt)}
          </Text>
        </Field>
        <Field label="대상 업무">
          <Text size="md" data-testid="suspension-target-works">
            {s.targetWorks.map((w) => w.name).join(', ')}
          </Text>
        </Field>
        <Field label="관련 업무">
          <Text size="md" data-testid="suspension-businesses">
            {s.businesses.length > 0
              ? s.businesses.map((b) => b.label).join(', ')
              : '없음(결제원 운영·상황실·대외계가 확인)'}
          </Text>
        </Field>
        <Field label="중단 이유(작업 내용)">
          <Body text={s.reason} />
        </Field>
        <Field label="결제원 조치사항">
          <Body text={s.kftcAction} />
        </Field>
        <Field label="담당자 연락처">
          <Stack gap={4} data-testid="suspension-contacts">
            {s.contacts.map((c, i) => (
              <Text key={i} size="md">
                {c.name} · {formatPhone(c.phone)}
                {c.email && ` · ${c.email}`}
              </Text>
            ))}
          </Stack>
        </Field>
        <Field label="추가 사항">
          <Body text={s.extraNote} />
        </Field>
        <Field label="공문">
          <AttachmentPanel
            ownerType="SUSPENSION"
            ownerId={s.id}
            editable={s.permissions.edit}
            uploadLabel="공문 올리기"
            emptyMessage="첨부한 공문이 없습니다."
          />
        </Field>
      </Stack>
    </Card>
  );
}

/** 검토·확정 기록: 확인을 시작한 결제원 담당자와 조치 및 인수인계 사항. */
function ReviewCard({ suspension: s }: { suspension: SuspensionDetail }) {
  return (
    <Card>
      <Stack gap="md">
        <Text fw={700} size="xl">
          검토와 확정
        </Text>
        <Field label="확인한 결제원 담당자">
          {s.reviewers.length === 0 ? (
            <Text size="md" c="dimmed">
              아직 확인을 시작한 결제원 담당자가 없습니다.
            </Text>
          ) : (
            <Stack gap={6} data-testid="suspension-reviewers">
              {s.reviewers.map((r) => (
                <Group key={`${r.name}-${r.startedAt}`} gap="sm" wrap="nowrap">
                  <ThemeIcon variant="light" color="sky" radius="sm" size={28}>
                    <Text size="xs" fw={700}>
                      {r.name.slice(0, 1)}
                    </Text>
                  </ThemeIcon>
                  <Text size="md">
                    {r.name}{' '}
                    <Text span size="sm" c="dimmed">
                      {r.roleLabel} · {formatDateTime(r.startedAt)}
                    </Text>
                  </Text>
                </Group>
              ))}
            </Stack>
          )}
        </Field>
        <Field label="조치 및 인수인계 사항">
          {s.handover ? (
            <Stack gap={4}>
              <Text size="md" className={classes.content} data-testid="suspension-handover">
                {s.handover}
              </Text>
              {s.confirmedAt && (
                <Text size="xs" c="dimmed">
                  {formatAuthor('결제원', s.confirmedByName, s.confirmedByJobTitle)} ·{' '}
                  {formatDateTime(s.confirmedAt)} 확정
                </Text>
              )}
            </Stack>
          ) : (
            <Text size="md" c="dimmed">
              업무·개발·운영 담당자가 쓰면 확정됩니다.
            </Text>
          )}
        </Field>
        {s.handover && s.status === 'PENDING_REVIEW' && (
          <Alert variant="light" color="amber">
            기관이 내용을 고쳐 다시 확인이 필요합니다. 위 사항은 고치기 전에 쓴 것입니다.
          </Alert>
        )}
        {s.completedAt && (
          <Text size="sm" c="dimmed">
            {formatDateTime(s.completedAt)} 완료
          </Text>
        )}
      </Stack>
    </Card>
  );
}

/** 조치 및 인수인계 사항을 쓰고 확정한다. 확정한 건이면 쓴 사항을 고친다. 검증 메시지는 서버 것을 그대로 보인다. */
function HandoverModal({
  opened,
  onClose,
  suspension,
}: {
  opened: boolean;
  onClose: () => void;
  suspension: SuspensionDetail;
}) {
  const confirm = useConfirmSuspension(suspension.id);
  const form = useForm({ initialValues: { handover: suspension.handover ?? '' } });
  const [generalError, setGeneralError] = useState<string>();
  const close = () => {
    form.setValues({ handover: suspension.handover ?? '' });
    form.clearErrors();
    setGeneralError(undefined);
    onClose();
  };
  const confirmed = suspension.status === 'CONFIRMED';
  return (
    <Modal
      opened={opened}
      onClose={close}
      title={confirmed ? '조치 및 인수인계 사항 고치기' : '조치 쓰고 확정'}
      centered
      size="lg"
    >
      <form
        onSubmit={form.onSubmit(({ handover }) =>
          confirm.mutate(handover, {
            onSuccess: () => {
              setGeneralError(undefined);
              onClose();
            },
            onError: (error) => {
              const fields = fieldErrorsOf(error);
              form.setErrors(fields);
              setGeneralError(fields.handover ? undefined : error.message);
            },
          }),
        )}
      >
        <Stack gap="md">
          <Text size="md">
            {confirmed
              ? '확정한 일시중단의 조치 및 인수인계 사항을 고칩니다.'
              : '중단 동안 결제원이 할 조치와 다음 담당자에게 넘길 내용을 적습니다. 저장하면 확정되고 등록한 사람에게 알림이 갑니다.'}
          </Text>
          <Textarea
            label="조치 및 인수인계 사항"
            placeholder="예: 운영팀이 02:00 에 조회를 차단하고 재개 연락을 받으면 풉니다."
            autosize
            minRows={4}
            maxLength={2000}
            withAsterisk
            data-autofocus
            {...form.getInputProps('handover')}
          />
          {generalError && (
            <Alert variant="light" color="red">
              {generalError}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={close}>
              닫기
            </Button>
            <Button type="submit" loading={confirm.isPending}>
              {confirmed ? '저장' : '확정하기'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
