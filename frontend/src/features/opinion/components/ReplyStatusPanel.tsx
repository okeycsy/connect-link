import {
  Alert,
  Badge,
  Box,
  Button,
  Card,
  Group,
  SimpleGrid,
  Skeleton,
  Stack,
  Table,
  Text,
} from '@mantine/core';
import { IconMailForward } from '@tabler/icons-react';
import { useState } from 'react';

import { formatDateTime } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import { DetailDrawer } from '../../../shared/list/DetailDrawer';
import { TableEmpty } from '../../../shared/list/TableEmpty';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import {
  type OpinionCollation,
  type OpinionCollationRow,
  type OpinionRequestDetail,
  useOpinionCollation,
  useReviewOpinionReplyRevision,
  useSendOpinionReminder,
} from '../hooks';
import { ReplyStatusBadge, ReplyTypeBadge } from './OpinionBadges';
import { RemindModal, ReasonModal } from './OpinionModals';
import classes from './OpinionTable.module.css';

type ReplyFilter = 'ALL' | 'REPLIED' | 'NOT_REPLIED';

const TYPE_CLASS = {
  AGREE: classes.agree,
  CONDITIONAL: classes.conditional,
  DISAGREE: classes.disagree,
} as const;

/**
 * 회신 현황(결제원 담당자). 제출 기관 수·회신율, 의견 유형별 기관 수와 이름, 기관별 회신 상태. 미회신만 걸러 리마인드 메일을 보내고,
 * 행을 누르면 회신 내용과 근거자료, 수정 요청 승인·반려를 본다.
 */
export function ReplyStatusPanel({ request }: { request: OpinionRequestDetail }) {
  const { data, isPending, error } = useOpinionCollation(request.id);
  const [filter, setFilter] = useState<ReplyFilter>('ALL');
  const [selected, setSelected] = useState<number | null>(null);
  const [remindOpen, setRemindOpen] = useState(false);
  const remind = useSendOpinionReminder(request.id);
  const [sentMessage, setSentMessage] = useState<string>();

  if (error) return <StateView kind={stateKindOf(error)} compact />;
  if (isPending || !data) return <Skeleton h={320} />;

  const rows = data.rows.filter((r) =>
    filter === 'ALL' ? true : filter === 'REPLIED' ? r.replied : !r.replied,
  );
  const notReplied = data.rows.filter((r) => !r.replied);
  const open = request.status === 'OPEN';
  const selectedRow = data.rows.find((r) => r.targetId === selected) ?? null;

  return (
    <Stack gap="md">
      <SummaryCards collation={data} />

      <Card>
        <Group justify="space-between" mb="sm" gap="sm">
          <ChipFilter<ReplyFilter>
            label="회신"
            value={filter}
            onChange={setFilter}
            options={[
              { value: 'ALL', label: `전체 ${data.summary.targetCount}` },
              { value: 'REPLIED', label: `회신 ${data.summary.repliedCount}` },
              { value: 'NOT_REPLIED', label: `미회신 ${data.summary.notRepliedCount}` },
            ]}
          />
          {open && (
            <Button
              variant="light"
              leftSection={<IconMailForward size={18} stroke={1.8} />}
              disabled={notReplied.length === 0}
              onClick={() => {
                remind.reset();
                setRemindOpen(true);
              }}
            >
              미회신 기관 리마인드
            </Button>
          )}
        </Group>
        {sentMessage && (
          <Alert
            variant="light"
            color="sky"
            mb="sm"
            withCloseButton
            onClose={() => setSentMessage(undefined)}
            data-testid="remind-sent"
          >
            {sentMessage}
          </Alert>
        )}
        <Table verticalSpacing="sm" highlightOnHover data-testid="reply-status-table">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>기관</Table.Th>
              <Table.Th w={120}>회신 양식</Table.Th>
              <Table.Th w={180}>회신 담당자</Table.Th>
              <Table.Th w={110}>상태</Table.Th>
              <Table.Th w={120}>의견 유형</Table.Th>
              <Table.Th w={150}>제출 일시</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((r) => (
              <Table.Tr
                key={r.targetId}
                className={classes.row}
                data-selected={r.targetId === selected || undefined}
                tabIndex={0}
                role="button"
                aria-label={`${r.organizationName} 회신`}
                onClick={() => setSelected(r.targetId)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    setSelected(r.targetId);
                  }
                }}
              >
                <Table.Td>
                  <Group gap={6}>
                    <Text fw={600}>{r.organizationName}</Text>
                    {!r.replied && (
                      <Badge size="xs" color="amber">
                        미회신
                      </Badge>
                    )}
                  </Group>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{r.formName ?? '-'}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm" c={r.recipientNames.length ? undefined : 'dimmed'}>
                    {r.recipientNames.length ? r.recipientNames.join(', ') : '지정 안 함'}
                  </Text>
                </Table.Td>
                <Table.Td>
                  <ReplyStatusBadge status={r.replyStatus} size="xs" />
                </Table.Td>
                <Table.Td>
                  <ReplyTypeBadge type={r.type} size="xs" />
                </Table.Td>
                <Table.Td>
                  <Text size="sm" className={classes.numeric}>
                    {formatDateTime(r.submittedAt)}
                  </Text>
                </Table.Td>
              </Table.Tr>
            ))}
            {rows.length === 0 && <TableEmpty colSpan={6} message="해당하는 기관이 없습니다." />}
          </Table.Tbody>
        </Table>
      </Card>

      {remindOpen && (
        <RemindModal
          opened
          onClose={() => setRemindOpen(false)}
          rows={notReplied}
          pending={remind.isPending}
          error={remind.error}
          onSend={(ids) =>
            remind.mutate(ids, {
              onSuccess: (r) => {
                setRemindOpen(false);
                setSentMessage(
                  `${r.organizationCount}곳 ${r.recipientCount}명에게 리마인드 메일을 보냈습니다.`,
                );
              },
            })
          }
        />
      )}

      <ReplyDrawer request={request} row={selectedRow} onClose={() => setSelected(null)} />
    </Stack>
  );
}

/** 위쪽 요약: 제출 기관 수, 회신율, 의견 유형별 막대와 기관 이름. */
function SummaryCards({ collation }: { collation: OpinionCollation }) {
  const { summary } = collation;
  const total = Math.max(summary.targetCount, 1);
  return (
    <SimpleGrid cols={{ base: 1, md: 3 }} spacing="md">
      <Card>
        <Text size="sm" c="dimmed" fw={600}>
          제출 기관
        </Text>
        <Text fw={700} fz={32} className={classes.numeric} data-testid="replied-count">
          {summary.repliedCount}
          <Text span c="dimmed" fz="lg" fw={500}>
            {' '}
            / {summary.targetCount}곳
          </Text>
        </Text>
        <Text size="sm" c="dimmed">
          미회신 {summary.notRepliedCount}곳
        </Text>
      </Card>
      <Card>
        <Text size="sm" c="dimmed" fw={600}>
          회신율
        </Text>
        <Text fw={700} fz={32} c="clay.6" className={classes.numeric} data-testid="reply-rate">
          {summary.replyRate}%
        </Text>
        <Text size="sm" c="dimmed">
          전체 대상 기관 대비 제출 기관
        </Text>
      </Card>
      <Card>
        <Text size="sm" c="dimmed" fw={600} mb={8}>
          의견 유형
        </Text>
        <div className={classes.bar} role="img" aria-label="의견 유형 분포">
          {summary.byType.map((t) => (
            <div
              key={t.type}
              className={`${classes.segment} ${TYPE_CLASS[t.type]}`}
              style={{ width: `${(t.count * 100) / total}%` }}
            />
          ))}
        </div>
        <Stack gap={4} mt="sm">
          {summary.byType.map((t) => (
            <Group key={t.type} gap={6} wrap="nowrap" align="flex-start">
              <Box component="span" className={`${classes.dot} ${TYPE_CLASS[t.type]}`} mt={5} />
              <Text size="sm" fw={600} w={80} data-testid={`type-${t.type}`}>
                {t.label} {t.count}
              </Text>
              <Text size="sm" c="dimmed" lineClamp={2}>
                {t.organizations.join(', ') || '-'}
              </Text>
            </Group>
          ))}
          <Group gap={6} wrap="nowrap" align="flex-start">
            <Box component="span" className={`${classes.dot} ${classes.none}`} mt={5} />
            <Text size="sm" fw={600} w={80}>
              미회신 {summary.notRepliedCount}
            </Text>
            <Text size="sm" c="dimmed" lineClamp={2}>
              {summary.notRepliedOrganizations.join(', ') || '-'}
            </Text>
          </Group>
        </Stack>
      </Card>
    </SimpleGrid>
  );
}

/** 기관 회신 상세 패널. 제출한 내용과 근거자료, 수정 요청이 있으면 승인·반려 버튼. */
function ReplyDrawer({
  request,
  row,
  onClose,
}: {
  request: OpinionRequestDetail;
  row: OpinionCollationRow | null;
  onClose: () => void;
}) {
  const review = useReviewOpinionReplyRevision(request.id);
  const [rejectOpen, setRejectOpen] = useState(false);
  const revision = row?.replyStatus === 'REVISION_REQUESTED' && request.status === 'OPEN';

  return (
    <>
      <DetailDrawer
        opened={row != null}
        onClose={() => {
          review.reset();
          onClose();
        }}
        title={row?.organizationName ?? ''}
        subtitle={row?.formName ? `회신 양식 ${row.formName}` : '회신 양식 없음'}
        footer={
          revision && row?.replyId != null ? (
            <>
              <Button color="red" variant="light" onClick={() => setRejectOpen(true)}>
                수정 요청 반려
              </Button>
              <Button
                loading={review.isPending}
                onClick={() => review.mutate({ replyId: row.replyId!, approve: true })}
              >
                수정 승인
              </Button>
            </>
          ) : undefined
        }
      >
        {row && (
          <Stack gap="md">
            <Group gap="xs">
              <ReplyStatusBadge status={row.replyStatus} />
              <ReplyTypeBadge type={row.type} />
            </Group>
            {revision && (
              <Alert variant="light" color="clay" title="수정 요청" data-testid="revision-request">
                <Text size="sm" className={classes.content}>
                  {row.revisionReason}
                </Text>
                <Text size="xs" c="dimmed" mt={4}>
                  {formatDateTime(row.revisionRequestedAt)}
                </Text>
              </Alert>
            )}
            {review.isError && (
              <Alert variant="light" color="red">
                {review.error.message}
              </Alert>
            )}
            {row.replied ? (
              <>
                <div>
                  <Text size="sm" c="dimmed" fw={600}>
                    세부의견
                  </Text>
                  <Text size="md" className={classes.content}>
                    {row.content ?? '(적지 않음)'}
                  </Text>
                </div>
                <Text size="sm" c="dimmed">
                  {row.replierName} · {formatDateTime(row.submittedAt)} 제출
                </Text>
              </>
            ) : (
              <Text c="dimmed">아직 제출하지 않았습니다.</Text>
            )}
            {row.replyId != null && row.replied && (
              <div>
                <Text size="sm" c="dimmed" fw={600} mb={4}>
                  근거자료
                </Text>
                <AttachmentPanel
                  ownerType="OPINION_REPLY"
                  ownerId={row.replyId}
                  emptyMessage="근거자료가 없습니다."
                />
              </div>
            )}
          </Stack>
        )}
      </DetailDrawer>
      {row?.replyId != null && (
        <ReasonModal
          opened={rejectOpen}
          onClose={() => setRejectOpen(false)}
          title="수정 요청 반려"
          message={`${row.organizationName}의 수정 요청을 받아들이지 않습니다. 회신은 제출한 내용 그대로 남고, 사유가 기관 담당자에게 갑니다.`}
          label="반려 사유"
          confirmLabel="반려하기"
          danger
          pending={review.isPending}
          onConfirm={(reason, done) =>
            review.mutate({ replyId: row.replyId!, approve: false, reason }, done)
          }
        />
      )}
    </>
  );
}
