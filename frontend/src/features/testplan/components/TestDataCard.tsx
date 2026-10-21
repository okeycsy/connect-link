import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  SegmentedControl,
  Skeleton,
  Stack,
  Text,
  Textarea,
} from '@mantine/core';
import { IconCheck } from '@tabler/icons-react';
import { useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import { formatDateTime } from '../../../shared/format';
import { ErrorState } from '../../../shared/state/StateView';
import { AttachmentPanel } from '../../attachment/components/AttachmentPanel';
import { type TestData, type TestDataStatus, useSaveTestData, useTestData } from '../hooks';

const STATUS_OPTIONS: { value: TestDataStatus; label: string }[] = [
  { value: 'NOT_CREATED', label: '미생성' },
  { value: 'CREATED', label: '생성 완료' },
];

/** 생성 여부 배지. 기관별 현황 표와 함께 쓴다. */
export function TestDataBadge({ status, label }: { status: TestDataStatus; label: string }) {
  return (
    <Badge color={status === 'CREATED' ? 'sky' : 'gray'} variant="light">
      {label}
    </Badge>
  );
}

/**
 * 테스트 전 대상 기관 한 곳의 테스트데이터 준비. 기관 담당자는 결제원이 지정한 회신 양식을 내려받아 테스트데이터를 만들고, 생성 여부·메모를 저장하고
 * 만든 파일을 올린다. 결제원 담당자는 같은 내용을 읽기만 한다.
 */
export function TestDataCard({
  planId,
  organizationId,
}: {
  planId: number;
  organizationId: number;
}) {
  const { data, isError, error } = useTestData(planId, organizationId);
  if (isError) return <ErrorState error={error} />;
  if (!data) return <Skeleton h={160} />;
  return <TestDataView key={`${data.status}-${data.note ?? ''}`} planId={planId} data={data} />;
}

function TestDataView({ planId, data }: { planId: number; data: TestData }) {
  const save = useSaveTestData(planId, data.organizationId);
  const [status, setStatus] = useState<TestDataStatus>(data.status);
  const [note, setNote] = useState(data.note ?? '');
  const dirty = status !== data.status || note.trim() !== (data.note ?? '');
  const errors = fieldErrorsOf(save.error);

  return (
    <Card data-testid="test-data">
      <Stack gap="md">
        <Group justify="space-between" align="center" gap="sm">
          <Group gap="xs">
            <Text fw={700} size="xl">
              테스트데이터 준비
            </Text>
            <TestDataBadge status={data.status} label={data.statusLabel} />
          </Group>
          {data.reportedAt && (
            <Text size="sm" c="dimmed">
              {data.reportedByName} · {formatDateTime(data.reportedAt)}
            </Text>
          )}
        </Group>

        <div>
          <Text size="sm" c="dimmed" fw={600} mb={4}>
            결제원 회신 양식
          </Text>
          <AttachmentPanel
            ownerType="TEST_DATA_FORM"
            ownerId={planId}
            emptyMessage="지정한 양식이 없습니다. 기관에서 정한 형식으로 올려 주세요."
          />
        </div>

        {data.canEdit ? (
          <Stack gap="sm">
            <Text size="sm" c="dimmed">
              테스트 전에 테스트데이터를 만들었는지 알려 주세요. 생성 완료로 저장하면 결제원 알림
              대상 담당자에게 매터모스트 알림이 갑니다.
            </Text>
            <SegmentedControl
              aria-label="테스트데이터 생성 여부"
              w={{ base: '100%', sm: 240 }}
              data={STATUS_OPTIONS}
              value={status}
              onChange={(v) => setStatus(v as TestDataStatus)}
            />
            <Textarea
              label="메모"
              placeholder="만든 테스트데이터 설명(계좌 수, 시나리오 등)이나 아직 못 만든 이유"
              autosize
              minRows={2}
              maxLength={2000}
              value={note}
              onChange={(e) => setNote(e.currentTarget.value)}
              error={errors.note}
            />
            {save.isError && !errors.note && (
              <Alert variant="light" color="red">
                {save.error.message}
              </Alert>
            )}
            <Group justify="flex-end">
              <Button
                leftSection={<IconCheck size={16} stroke={1.8} />}
                disabled={!dirty}
                loading={save.isPending}
                onClick={() => save.mutate({ status, note: note.trim() || null })}
              >
                테스트데이터 저장
              </Button>
            </Group>
          </Stack>
        ) : (
          <div>
            <Text size="sm" c="dimmed" fw={600} mb={2}>
              메모
            </Text>
            <Text size="md" c={data.note ? undefined : 'dimmed'} style={{ whiteSpace: 'pre-wrap' }}>
              {data.note ?? '없음'}
            </Text>
          </div>
        )}

        <div>
          <Text size="sm" c="dimmed" fw={600} mb={4}>
            {data.canEdit ? '만든 테스트데이터 파일' : `${data.organizationName} 테스트데이터 파일`}
          </Text>
          <AttachmentPanel
            ownerType="TEST_DATA"
            ownerId={data.targetId}
            editable={data.canEdit}
            uploadLabel="테스트데이터 올리기"
            emptyMessage="올린 테스트데이터가 없습니다."
          />
        </div>
      </Stack>
    </Card>
  );
}
