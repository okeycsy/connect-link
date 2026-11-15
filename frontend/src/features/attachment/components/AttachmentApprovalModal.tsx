import {
  Anchor,
  Button,
  Group,
  Modal,
  Skeleton,
  Stack,
  Text,
  Textarea,
  ThemeIcon,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconFile } from '@tabler/icons-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { formatBytes, formatDateTime } from '../../../shared/format';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import {
  attachmentDownloadUrl,
  useApproveAttachments,
  useAttachmentApproval,
  useFileTransferBlocked,
  useRejectAttachments,
  type AttachmentApproval,
  type AttachmentOwner,
} from '../hooks';
import { ApprovalStatusBadge } from './ApprovalStatusBadge';
import { FileTransferBlockedNote } from './FileTransferBlockedNote';

interface Props {
  /** 결재할 등록 한 건(첨부 대상). null 이면 닫혀 있다. */
  owner: AttachmentOwner | null;
  /** 대상 화면 링크에 쓸 업무 영역(대상에 업무가 없을 때). */
  base: string;
  onClose: () => void;
}

/**
 * 첨부파일 반출 결재 창. 결제원 담당자가 공지·의견 요청·기관 테스트 등을 등록하며 붙인 파일을 같은 팀 팀장이 한 번에 승인하거나 반려한다. 팀장은
 * 알림 센터에서 결재 요청 알림을 눌러 연다. 파일을 열어 보고 결재하는 것은 내부망에서만 된다.
 */
export function AttachmentApprovalModal({ owner, base, onClose }: Props) {
  const { data, isPending, error } = useAttachmentApproval(owner);
  const blocked = useFileTransferBlocked();
  const approve = useApproveAttachments();
  const reject = useRejectAttachments();
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string>();

  const close = () => {
    setRejecting(false);
    setReason('');
    setReasonError(undefined);
    onClose();
  };

  const fail = (title: string, e: unknown) =>
    notifications.show({
      title,
      message: e instanceof ApiError ? e.message : '잠시 후 다시 시도해 주세요.',
      color: 'red',
    });

  const pending = data?.files.filter((f) => f.status === 'PENDING') ?? [];

  const onApprove = async () => {
    if (!owner) return;
    try {
      await approve.mutateAsync({ owner, input: undefined });
      notifications.show({
        message: `첨부파일 ${pending.length}개를 승인했습니다.`,
        color: 'clay',
      });
      close();
    } catch (e) {
      fail('승인하지 못했습니다', e);
    }
  };

  const onReject = async () => {
    if (!owner) return;
    if (reason.trim() === '') {
      setReasonError('반려 사유를 적어 주세요.');
      return;
    }
    try {
      await reject.mutateAsync({ owner, input: reason.trim() });
      notifications.show({ message: '첨부파일을 반려했습니다.', color: 'clay' });
      close();
    } catch (e) {
      const fieldError = fieldErrorsOf(e).reason;
      if (fieldError) {
        setReasonError(fieldError);
      } else {
        fail('반려하지 못했습니다', e);
      }
    }
  };

  const ownerLink =
    data?.ownerPath != null ? `/${data.ownerBusiness ?? base}/${data.ownerPath}` : null;
  const uploader = pending[0] ?? data?.files[0];

  return (
    <Modal opened={owner !== null} onClose={close} title="첨부파일 반출 결재" centered size="lg">
      <Stack gap="sm" data-testid="attachment-approval">
        {error ? (
          <StateView
            kind={stateKindOf(error)}
            compact
            title={
              error instanceof ApiError && error.status === 404
                ? '결재할 첨부파일이 없습니다'
                : undefined
            }
          />
        ) : isPending || !data ? (
          <Stack gap="xs">
            <Skeleton h={20} w="60%" />
            <Skeleton h={64} />
          </Stack>
        ) : (
          <>
            <Stack gap={2}>
              <Text size="sm" c="dimmed">
                {data.ownerTypeLabel}
              </Text>
              {ownerLink ? (
                <Anchor component={Link} to={ownerLink} fw={700} onClick={close}>
                  {data.ownerTitle}
                </Anchor>
              ) : (
                <Text fw={700}>{data.ownerTitle}</Text>
              )}
              {uploader && (
                <Text size="sm" c="dimmed">
                  올린 사람 {uploader.uploaderName ?? uploader.uploaderEmail}
                  {uploader.uploaderTeam ? ` · ${uploader.uploaderTeam}` : ''}
                </Text>
              )}
            </Stack>

            <Stack gap={8} role="list" aria-label="결재할 첨부파일">
              {data.files.map((f) => (
                <FileRow key={f.id} file={f} downloadable={!blocked} />
              ))}
            </Stack>

            {blocked && <FileTransferBlockedNote />}

            {pending.length === 0 ? (
              <Text size="sm" c="dimmed">
                이미 결재한 건이에요.
              </Text>
            ) : rejecting ? (
              <Stack gap="xs">
                <Textarea
                  label="반려 사유"
                  description={`사유는 올린 사람(${uploader?.uploaderName ?? ''})에게 알림으로 갑니다.`}
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
                  <Button variant="subtle" color="gray" onClick={() => setRejecting(false)}>
                    취소
                  </Button>
                  <Button color="red" loading={reject.isPending} onClick={() => void onReject()}>
                    반려
                  </Button>
                </Group>
              </Stack>
            ) : (
              <Group justify="flex-end" gap="xs">
                <Button
                  variant="light"
                  color="red"
                  disabled={blocked}
                  onClick={() => setRejecting(true)}
                >
                  반려
                </Button>
                <Button
                  loading={approve.isPending}
                  disabled={blocked}
                  onClick={() => void onApprove()}
                >
                  {pending.length > 1 ? `${pending.length}개 모두 승인` : '승인'}
                </Button>
              </Group>
            )}
          </>
        )}
      </Stack>
    </Modal>
  );
}

function FileRow({ file, downloadable }: { file: AttachmentApproval; downloadable: boolean }) {
  return (
    <Group
      gap="sm"
      wrap="nowrap"
      align="flex-start"
      role="listitem"
      px="sm"
      py={10}
      data-testid="approval-file"
      style={{
        border: '1px solid var(--mantine-color-default-border)',
        borderRadius: 'var(--mantine-radius-sm)',
      }}
    >
      <ThemeIcon variant="light" color="clay" size={32} radius="sm">
        <IconFile size={18} stroke={1.6} />
      </ThemeIcon>
      <Stack gap={2} style={{ flex: 1, minWidth: 0 }}>
        <Group gap={8} wrap="nowrap">
          {downloadable ? (
            <Anchor
              href={attachmentDownloadUrl(file.id)}
              download={file.name}
              size="sm"
              fw={600}
              truncate="end"
            >
              {file.name}
            </Anchor>
          ) : (
            <Text size="sm" fw={600} truncate="end">
              {file.name}
            </Text>
          )}
          <ApprovalStatusBadge status={file.status} />
        </Group>
        <Text size="xs" c="dimmed">
          {formatBytes(file.size)} · {formatDateTime(file.uploadedAt)}
          {file.decidedAt && file.status !== 'PENDING'
            ? ` · ${formatDateTime(file.decidedAt)} 결재`
            : ''}
        </Text>
        {file.status === 'REJECTED' && file.rejectionReason && (
          <Text size="xs" c="red">
            반려 사유: {file.rejectionReason}
          </Text>
        )}
      </Stack>
    </Group>
  );
}
