import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Modal,
  NumberInput,
  Paper,
  Progress,
  SegmentedControl,
  Skeleton,
  Stack,
  Text,
  Textarea,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { IconCheck, IconMessage } from '@tabler/icons-react';
import { useEffect, useMemo, useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import { formatDateTime } from '../../../shared/format';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import { ErrorState } from '../../../shared/state/StateView';
import {
  type TestItemResultRow,
  type TestResult,
  type TestTargetSummary,
  useSaveTestFeedback,
  useSaveTestResults,
  useTestTargetResults,
} from '../hooks';
import { RESULT_OPTIONS } from '../results';
import { TestResultBadge } from './TestPlanBadges';
import classes from './TestPlanTable.module.css';

interface Draft {
  result: TestResult | null;
  note: string;
}

const draftOf = (row: TestItemResultRow): Draft => ({
  result: row.result ?? null,
  note: row.note ?? '',
});

const changed = (row: TestItemResultRow, draft: Draft | undefined) =>
  draft != null &&
  draft.result != null &&
  (draft.result !== (row.result ?? null) || draft.note.trim() !== (row.note ?? ''));

/**
 * 대상 기관 한 곳의 요구사항별 결과. 기관 담당자는 결과(정상·오류·수행중·미수행)와 특이사항을 고르고 한 번에 저장한다. 결제원 담당자는 기관 결과를
 * 보고 요구사항마다 의견(미니 수행률 0~100·특이사항)을 남긴다. 기관 정상 + 결제원 100% 면 그 요구사항은 수행 완료다.
 */
export function TargetResultsCard({
  planId,
  organizationId,
}: {
  planId: number;
  organizationId: number;
}) {
  const { data, isError, error } = useTestTargetResults(planId, organizationId);
  const save = useSaveTestResults(planId, organizationId);
  // 기관 담당자가 고친 요구사항만 담는다. 다시 읽어도 저장 전 입력은 남고, 저장하면 비운다.
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [feedbackRow, setFeedbackRow] = useState<TestItemResultRow | null>(null);
  // 저장한 묶음의 순서. 서버 오류 경로 results[i] 를 요구사항으로 되돌린다.
  const [sentItemIds, setSentItemIds] = useState<number[]>([]);

  const rowErrors = useMemo(() => {
    const errors: Record<number, string> = {};
    for (const [path, message] of Object.entries(fieldErrorsOf(save.error))) {
      const m = /^results\.(\d+)\./.exec(path);
      const itemId = m ? sentItemIds[Number(m[1])] : undefined;
      if (itemId != null) errors[itemId] = message;
    }
    return errors;
  }, [save.error, sentItemIds]);

  if (isError) return <ErrorState error={error} />;
  if (!data) return <Skeleton h={240} />;

  const dirty = data.rows.filter((r) => changed(r, drafts[r.itemId]));
  const submit = () => {
    const entries = dirty.map((r) => ({
      itemId: r.itemId,
      result: drafts[r.itemId]!.result!,
      note: drafts[r.itemId]!.note.trim() || null,
    }));
    setSentItemIds(entries.map((e) => e.itemId));
    save.mutate({ results: entries }, { onSuccess: () => setDrafts({}) });
  };
  const generalError =
    save.isError && Object.keys(rowErrors).length === 0 ? save.error.message : undefined;

  return (
    <Card data-testid="target-results">
      <Stack gap="md">
        <TargetHeader summary={data.summary} manager={data.canFeedback} />
        {data.canReport && (
          <Group justify="space-between" gap="sm">
            <Text size="sm" c="dimmed">
              결과를 고르고 저장하면 결제원 알림 대상 담당자에게 매터모스트 알림이 갑니다. 오류면
              특이사항에 오류내용을 적어 주세요.
            </Text>
            <Button
              leftSection={<IconCheck size={16} stroke={1.8} />}
              disabled={dirty.length === 0}
              loading={save.isPending}
              onClick={submit}
            >
              결과 저장{dirty.length > 0 ? ` (${dirty.length})` : ''}
            </Button>
          </Group>
        )}
        {generalError && (
          <Alert variant="light" color="red">
            {generalError}
          </Alert>
        )}
        <Stack gap={8}>
          {data.rows.map((row) => (
            <ResultRow
              key={row.itemId}
              row={row}
              draft={drafts[row.itemId] ?? draftOf(row)}
              editable={data.canReport}
              feedback={data.canFeedback}
              error={rowErrors[row.itemId]}
              onDraft={(d) => setDrafts((prev) => ({ ...prev, [row.itemId]: d }))}
              onFeedback={() => setFeedbackRow(row)}
              planId={planId}
              organizationId={organizationId}
            />
          ))}
        </Stack>
      </Stack>
      <FeedbackModal
        row={feedbackRow}
        onClose={() => setFeedbackRow(null)}
        planId={planId}
        organizationId={organizationId}
      />
    </Card>
  );
}

function TargetHeader({ summary: s, manager }: { summary: TestTargetSummary; manager: boolean }) {
  const counts = [
    { label: '정상', n: s.passed },
    { label: '오류', n: s.failed },
    { label: '수행중', n: s.inProgress },
    { label: '미수행', n: s.notStarted },
    { label: '미등록', n: s.unreported },
  ];
  return (
    <Group justify="space-between" align="center" gap="md">
      <Group gap="sm" wrap="nowrap">
        <OrgMonogram name={s.organizationName} type="FINANCE_ORGANIZATION" />
        <div style={{ minWidth: 0 }}>
          <Group gap="xs">
            <Text fw={700} size="xl">
              {s.organizationName}
            </Text>
            {s.completed && <Badge color="sky">수행 완료</Badge>}
            {manager && s.awaitingReview > 0 && (
              <Badge color="amber" data-testid="awaiting-review">
                확인할 결과 {s.awaitingReview}
              </Badge>
            )}
          </Group>
          <Text size="sm" c="dimmed" className={classes.counts}>
            {counts.map((c) => `${c.label} ${c.n}`).join(' · ')}
            {s.lastReportedAt && ` · 마지막 등록 ${formatDateTime(s.lastReportedAt)}`}
          </Text>
        </div>
      </Group>
      <Group gap="xs" wrap="nowrap">
        <Progress
          value={s.progressRate}
          size="md"
          radius="xl"
          color="clay"
          w={120}
          aria-label="기관 수행률"
        />
        <Text fw={700} size="lg" className={classes.numeric} data-testid="target-progress">
          {s.progressRate}%
        </Text>
        <Text size="sm" c="dimmed" className={classes.numeric}>
          완료 {s.completedCount}/{s.itemCount}
        </Text>
      </Group>
    </Group>
  );
}

function ResultRow({
  row,
  draft,
  editable,
  feedback,
  error,
  onDraft,
  onFeedback,
  planId,
  organizationId,
}: {
  row: TestItemResultRow;
  draft: Draft;
  editable: boolean;
  feedback: boolean;
  error: string | undefined;
  onDraft: (draft: Draft) => void;
  onFeedback: () => void;
  planId: number;
  organizationId: number;
}) {
  const quick = useSaveTestFeedback(planId, organizationId);
  const hasFeedback = row.feedbackRate != null || row.feedbackNote != null;
  return (
    <Paper
      p="md"
      radius="lg"
      bg={row.completed ? 'clay.0' : 'gray.1'}
      data-testid={`item-${row.number}`}
    >
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" wrap="nowrap" gap="sm">
          <Group gap="sm" align="flex-start" wrap="nowrap" style={{ flex: 1, minWidth: 0 }}>
            <Text fw={700} className={classes.numeric}>
              {row.number}
            </Text>
            <div>
              <Text fw={600}>{row.title}</Text>
              {row.description && (
                <Text size="sm" c="dimmed" className={classes.content}>
                  {row.description}
                </Text>
              )}
            </div>
          </Group>
          <Group gap={6} wrap="nowrap" style={{ flexShrink: 0 }}>
            {row.completed && (
              <Badge color="sky" data-testid="item-completed">
                테스트 수행 완료
              </Badge>
            )}
            {!editable && <TestResultBadge result={row.result} />}
          </Group>
        </Group>

        {editable ? (
          <Stack gap={6}>
            <SegmentedControl
              aria-label={`${row.number} 결과`}
              color="clay"
              radius="xl"
              size="sm"
              w="fit-content"
              data={RESULT_OPTIONS}
              value={draft.result ?? ''}
              onChange={(v) => onDraft({ ...draft, result: v as TestResult })}
            />
            <Textarea
              aria-label={`${row.number} 특이사항`}
              placeholder={draft.result === 'FAILED' ? '오류내용을 적어 주세요.' : '특이사항(선택)'}
              autosize
              minRows={1}
              maxLength={2000}
              value={draft.note}
              error={error}
              onChange={(e) => onDraft({ ...draft, note: e.currentTarget.value })}
            />
            {row.reportedAt && (
              <Text size="xs" c="dimmed">
                {row.reportedByName} · {formatDateTime(row.reportedAt)} 등록
              </Text>
            )}
          </Stack>
        ) : row.result ? (
          <Stack gap={2}>
            {row.note && (
              <Text size="sm" className={classes.content} data-testid="item-note">
                {row.note}
              </Text>
            )}
            <Text size="xs" c="dimmed">
              {row.reportedByName} · {row.reportedAt && formatDateTime(row.reportedAt)} 등록
            </Text>
          </Stack>
        ) : (
          <Text size="sm" c="dimmed">
            기관이 아직 결과를 등록하지 않았습니다.
          </Text>
        )}

        {(hasFeedback || (feedback && row.result)) && (
          <Paper p="sm" radius="lg" bg="var(--cl-card)" data-testid="item-feedback">
            <Group justify="space-between" align="flex-start" gap="sm" wrap="nowrap">
              <div>
                <Group gap="xs">
                  <Text size="sm" fw={600}>
                    결제원 의견
                  </Text>
                  {row.feedbackRate != null && (
                    <Text size="sm" fw={700} c="clay.6" className={classes.numeric}>
                      미니 수행률 {row.feedbackRate}%
                    </Text>
                  )}
                  {row.feedbackOutdated && (
                    <Badge size="sm" color="amber">
                      다시 확인
                    </Badge>
                  )}
                </Group>
                {hasFeedback ? (
                  <>
                    {row.feedbackNote && (
                      <Text size="sm" className={classes.content}>
                        {row.feedbackNote}
                      </Text>
                    )}
                    <Text size="xs" c="dimmed">
                      {row.feedbackByName} · {row.feedbackAt && formatDateTime(row.feedbackAt)}
                      {row.feedbackOutdated && ' · 기관이 그 뒤에 결과를 바꿨습니다'}
                    </Text>
                  </>
                ) : (
                  <Text size="sm" c="dimmed">
                    아직 남긴 의견이 없습니다.
                  </Text>
                )}
              </div>
              {feedback && row.result && (
                <Group gap={6} wrap="nowrap">
                  {row.result === 'PASSED' && !row.completed && (
                    <Button
                      size="xs"
                      variant="light"
                      leftSection={<IconCheck size={14} stroke={1.8} />}
                      loading={quick.isPending}
                      onClick={() =>
                        quick.mutate({ itemId: row.itemId, rate: 100, note: row.feedbackNote })
                      }
                    >
                      완료 확인
                    </Button>
                  )}
                  <Button
                    size="xs"
                    variant="default"
                    leftSection={<IconMessage size={14} stroke={1.8} />}
                    onClick={onFeedback}
                    aria-label={`${row.number} 의견`}
                  >
                    의견
                  </Button>
                </Group>
              )}
            </Group>
            {quick.isError && (
              <Text size="xs" c="red.6" mt={4}>
                {quick.error.message}
              </Text>
            )}
          </Paper>
        )}
      </Stack>
    </Paper>
  );
}

/** 결제원 의견 쓰기. 미니 수행률(0~100)과 특이사항, 둘 다 비우면 의견을 지운다. 검증 메시지는 서버 것을 그대로 보인다. */
function FeedbackModal({
  row,
  onClose,
  planId,
  organizationId,
}: {
  row: TestItemResultRow | null;
  onClose: () => void;
  planId: number;
  organizationId: number;
}) {
  const save = useSaveTestFeedback(planId, organizationId);
  const form = useForm<{ rate: number | string; note: string }>({
    initialValues: { rate: '', note: '' },
  });
  const [generalError, setGeneralError] = useState<string>();

  useEffect(() => {
    if (row) {
      form.setValues({ rate: row.feedbackRate ?? '', note: row.feedbackNote ?? '' });
      form.clearErrors();
      setGeneralError(undefined);
      save.reset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [row]);

  return (
    <Modal
      opened={row != null}
      onClose={onClose}
      title={row ? `${row.number} ${row.title} 결제원 의견` : ''}
      centered
      size="lg"
    >
      {row && (
        <form
          onSubmit={form.onSubmit(({ rate, note }) =>
            save.mutate(
              {
                itemId: row.itemId,
                rate: rate === '' ? null : Number(rate),
                note: note.trim() || null,
              },
              {
                onSuccess: onClose,
                onError: (error) => {
                  const fields = fieldErrorsOf(error);
                  form.setErrors(fields);
                  setGeneralError(Object.keys(fields).length === 0 ? error.message : undefined);
                },
              },
            ),
          )}
        >
          <Stack gap="md">
            <Group gap="xs">
              <Text size="sm">기관 결과</Text>
              <TestResultBadge result={row.result} />
              {row.note && (
                <Text size="sm" c="dimmed" lineClamp={1}>
                  {row.note}
                </Text>
              )}
            </Group>
            <NumberInput
              label="미니 수행률"
              description="결제원이 본 이 요구사항의 수행률입니다. 100% 이고 기관 결과가 정상이면 테스트 수행 완료입니다."
              min={0}
              max={100}
              suffix="%"
              allowDecimal={false}
              allowNegative={false}
              w={200}
              data-autofocus
              {...form.getInputProps('rate')}
            />
            <Textarea
              label="특이사항"
              placeholder="예: 003 전문에 대해 예외사항 발생"
              autosize
              minRows={3}
              maxLength={2000}
              {...form.getInputProps('note')}
            />
            <Text size="xs" c="dimmed">
              둘 다 비우고 저장하면 의견을 지웁니다. 결과를 등록한 기관 담당자에게 알림이 갑니다.
            </Text>
            {generalError && (
              <Alert variant="light" color="red">
                {generalError}
              </Alert>
            )}
            <Group justify="flex-end" gap="xs">
              <Button variant="default" onClick={onClose}>
                닫기
              </Button>
              <Button type="submit" loading={save.isPending}>
                저장
              </Button>
            </Group>
          </Stack>
        </form>
      )}
    </Modal>
  );
}
