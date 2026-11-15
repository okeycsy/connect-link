import {
  Anchor,
  Button,
  Card,
  Group,
  Modal,
  Pagination,
  SegmentedControl,
  Skeleton,
  Stack,
  Text,
  Textarea,
  ThemeIcon,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconFile } from '@tabler/icons-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { DEFAULT_BUSINESS_SLUG } from '../../../app/navigation';
import { PageHeader } from '../../../shared/PageHeader';
import { formatBytes, formatDateTime } from '../../../shared/format';
import { ChipFilter } from '../../../shared/list/ChipFilter';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import { ApprovalStatusBadge } from '../components/ApprovalStatusBadge';
import { FileTransferBlockedNote } from '../components/FileTransferBlockedNote';
import {
  attachmentDownloadUrl,
  useApproveAttachment,
  useAttachmentApprovals,
  useFileTransferBlocked,
  usePendingApprovalCount,
  useRejectAttachment,
  type AttachmentApproval,
  type AttachmentApprovalBox,
  type AttachmentApprovalStatus,
} from '../hooks';

type StatusFilter = 'ALL' | Exclude<AttachmentApprovalStatus, 'NOT_REQUIRED'>;

const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
  { value: 'PENDING', label: '결재 대기' },
  { value: 'APPROVED', label: '승인' },
  { value: 'REJECTED', label: '반려' },
  { value: 'ALL', label: '전체' },
];

/**
 * 파일 결재. 결제원 담당자가 올린 파일은 참가기관이 외부망에서 보게 되므로 외부망 반출이고, 같은 부서·팀 팀장이 승인해야 참가기관에 보인다.
 * 팀장은 "결재할 파일"에서 파일을 열어 보고 승인·반려하고(내부망에서만), 올린 사람은 "내가 올린 파일"에서 결재 현황을 본다.
 */
export function FileApprovalPage() {
  const { business: businessSlug } = useParams<{ business: string }>();
  const base = businessSlug ?? DEFAULT_BUSINESS_SLUG;
  const [box, setBox] = useState<AttachmentApprovalBox>('TO_APPROVE');
  const [status, setStatus] = useState<StatusFilter>('PENDING');
  const [page, setPage] = useState(0);
  const { data, isPending, error } = useAttachmentApprovals(
    box,
    status === 'ALL' ? undefined : status,
    page,
  );
  const { data: pending } = usePendingApprovalCount(true);
  const blocked = useFileTransferBlocked();
  const approve = useApproveAttachment();
  const reject = useRejectAttachment();
  const [rejecting, setRejecting] = useState<AttachmentApproval | null>(null);
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string>();

  const fail = (title: string, e: unknown) =>
    notifications.show({
      title,
      message: e instanceof ApiError ? e.message : '잠시 후 다시 시도해 주세요.',
      color: 'red',
    });

  const onApprove = async (item: AttachmentApproval) => {
    try {
      await approve.mutateAsync({ id: item.id, input: undefined });
      notifications.show({
        message: `${item.name} 반출을 승인했습니다. 이제 참가기관에 보입니다.`,
        color: 'clay',
      });
    } catch (e) {
      fail('승인하지 못했습니다', e);
    }
  };

  const onReject = async () => {
    if (!rejecting) return;
    if (reason.trim() === '') {
      setReasonError('반려 사유를 적어 주세요.');
      return;
    }
    try {
      await reject.mutateAsync({ id: rejecting.id, input: reason.trim() });
      notifications.show({ message: `${rejecting.name} 을(를) 반려했습니다.`, color: 'clay' });
      setRejecting(null);
    } catch (e) {
      const fieldError = fieldErrorsOf(e).reason;
      if (fieldError) {
        setReasonError(fieldError);
      } else {
        fail('반려하지 못했습니다', e);
      }
    }
  };

  const pendingCount = pending?.pending ?? 0;

  return (
    <Stack gap="md">
      <PageHeader
        title="파일 결재"
        description="결제원 담당자가 올린 파일은 외부망 반출이라 같은 팀 팀장님이 승인해야 참가기관에 보여요."
      />

      <Card data-testid="file-approvals">
        <Stack gap="sm">
          <Group justify="space-between" gap="sm">
            <SegmentedControl
              value={box}
              onChange={(v) => {
                const next = v as AttachmentApprovalBox;
                setBox(next);
                setStatus(next === 'TO_APPROVE' ? 'PENDING' : 'ALL');
                setPage(0);
              }}
              data={[
                {
                  value: 'TO_APPROVE',
                  label: pendingCount > 0 ? `결재할 파일 ${pendingCount}` : '결재할 파일',
                },
                { value: 'REQUESTED', label: '내가 올린 파일' },
              ]}
            />
            <ChipFilter
              label="상태"
              value={status}
              options={STATUS_OPTIONS}
              onChange={(v) => {
                setStatus(v);
                setPage(0);
              }}
            />
          </Group>
          {blocked && <FileTransferBlockedNote />}

          {error ? (
            <StateView kind={stateKindOf(error)} compact />
          ) : isPending || !data ? (
            <Stack gap="xs">
              <Skeleton h={72} />
              <Skeleton h={72} />
            </Stack>
          ) : data.content.length === 0 ? (
            <StateView
              kind="empty"
              compact
              title={box === 'TO_APPROVE' ? '결재할 파일이 없습니다' : '올린 파일이 없습니다'}
              description={
                box === 'TO_APPROVE'
                  ? '같은 팀 담당자가 파일을 올리면 여기와 알림으로 결재 요청이 옵니다.'
                  : '공지·의견 요청·기관 테스트 등에 올린 파일의 결재 현황이 여기 보입니다.'
              }
            />
          ) : (
            <Stack gap={8} role="list" aria-label="결재 파일">
              {data.content.map((item) => (
                <ApprovalRow
                  key={item.id}
                  item={item}
                  base={base}
                  box={box}
                  downloadable={!blocked}
                  onApprove={() => void onApprove(item)}
                  onReject={() => {
                    setRejecting(item);
                    setReason('');
                    setReasonError(undefined);
                  }}
                  approving={approve.isPending && approve.variables?.id === item.id}
                />
              ))}
            </Stack>
          )}
          {data && data.totalPages > 1 && (
            <Group justify="center">
              <Pagination
                total={data.totalPages}
                value={page + 1}
                onChange={(p) => setPage(p - 1)}
                size="sm"
              />
            </Group>
          )}
        </Stack>
      </Card>

      <Modal
        opened={rejecting !== null}
        onClose={() => setRejecting(null)}
        title="반출 반려"
        centered
      >
        <Stack gap="sm">
          <Text size="sm">
            {rejecting?.name} 을(를) 반려합니다. 사유는 올린 사람({rejecting?.uploaderName})에게
            알림으로 갑니다.
          </Text>
          <Textarea
            label="반려 사유"
            placeholder="예: 개인정보가 들어 있어요. 가린 뒤 다시 올려 주세요."
            autosize
            minRows={3}
            maxLength={500}
            value={reason}
            onChange={(e) => {
              setReason(e.currentTarget.value);
              setReasonError(undefined);
            }}
            error={reasonError}
            data-autofocus
          />
          <Group justify="flex-end" gap="xs">
            <Button variant="subtle" color="gray" onClick={() => setRejecting(null)}>
              취소
            </Button>
            <Button color="red" loading={reject.isPending} onClick={() => void onReject()}>
              반려
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}

function ApprovalRow({
  item,
  base,
  box,
  downloadable,
  onApprove,
  onReject,
  approving,
}: {
  item: AttachmentApproval;
  base: string;
  box: AttachmentApprovalBox;
  downloadable: boolean;
  onApprove: () => void;
  onReject: () => void;
  approving: boolean;
}) {
  const ownerLink = item.ownerPath ? `/${item.ownerBusiness ?? base}/${item.ownerPath}` : null;
  const decidable = box === 'TO_APPROVE' && item.status === 'PENDING';
  return (
    <Group
      gap="sm"
      wrap="nowrap"
      align="flex-start"
      role="listitem"
      px="sm"
      py={10}
      data-testid="file-approval-row"
      style={{
        border: '1px solid var(--mantine-color-default-border)',
        borderRadius: 'var(--mantine-radius-sm)',
      }}
    >
      <ThemeIcon variant="light" color="clay" size={36} radius="sm">
        <IconFile size={20} stroke={1.6} />
      </ThemeIcon>
      <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
        <Group gap={8} wrap="nowrap">
          {downloadable ? (
            <Anchor
              href={attachmentDownloadUrl(item.id)}
              download={item.name}
              size="sm"
              fw={600}
              truncate="end"
            >
              {item.name}
            </Anchor>
          ) : (
            <Text size="sm" fw={600} truncate="end">
              {item.name}
            </Text>
          )}
          <ApprovalStatusBadge status={item.status} />
        </Group>
        <Text size="xs" c="dimmed" truncate="end">
          {item.ownerTypeLabel} ·{' '}
          {ownerLink ? (
            <Anchor component={Link} to={ownerLink} size="xs">
              {item.ownerTitle}
            </Anchor>
          ) : (
            item.ownerTitle
          )}
        </Text>
        <Text size="xs" c="dimmed">
          {box === 'TO_APPROVE'
            ? `${item.uploaderName ?? item.uploaderEmail} · ${item.uploaderTeam ?? ''} · `
            : `결재자 ${item.approverName ?? '-'} 팀장 · `}
          {formatBytes(item.size)} · {formatDateTime(item.uploadedAt)}
        </Text>
        {item.status === 'APPROVED' && item.decidedAt && (
          <Text size="xs" c="dimmed">
            {formatDateTime(item.decidedAt)} 승인
          </Text>
        )}
        {item.status === 'REJECTED' && (
          <Text size="xs" c="red">
            반려 사유: {item.rejectionReason}
            {box === 'REQUESTED' && ' (그 화면에서 파일을 지우고 다시 올려 주세요)'}
          </Text>
        )}
        {decidable && (
          <Group gap="xs" mt={6}>
            <Button
              size="xs"
              onClick={onApprove}
              loading={approving}
              disabled={!downloadable}
              aria-label={`${item.name} 승인`}
            >
              승인
            </Button>
            <Button
              size="xs"
              variant="light"
              color="red"
              onClick={onReject}
              disabled={!downloadable}
              aria-label={`${item.name} 반려`}
            >
              반려
            </Button>
          </Group>
        )}
      </Stack>
    </Group>
  );
}
