import {
  ActionIcon,
  Anchor,
  Button,
  FileButton,
  Group,
  Popover,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
  Tooltip,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import {
  IconFile,
  IconFileSpreadsheet,
  IconFileText,
  IconFileTypePdf,
  IconPhoto,
  IconTrash,
  IconUpload,
} from '@tabler/icons-react';
import { useState } from 'react';

import { ApiError } from '../../../api/client';
import { formatBytes, formatDateTime } from '../../../shared/format';
import { StateView } from '../../../shared/state/StateView';
import { stateKindOf } from '../../../shared/state/stateKind';
import {
  attachmentDownloadUrl,
  useAttachments,
  useDeleteAttachment,
  useUploadAttachment,
  type Attachment,
  type AttachmentOwner,
} from '../hooks';

interface Props extends AttachmentOwner {
  /** 올리기·지우기 버튼을 보일지. 권한은 서버가 지키고 여기서는 버튼만 숨긴다. */
  editable?: boolean;
  /** 올리기 버튼 글자. */
  uploadLabel?: string;
  emptyMessage?: string;
}

/**
 * 첨부파일 목록 + 올리기·지우기. 의견 요청(첨부자료), 기관 회신(근거자료), 일시중단(공문)이 대상(ownerType, ownerId)만 바꿔 쓴다.
 * 내려받기는 링크라 브라우저가 원래 파일 이름으로 저장한다.
 */
export function AttachmentPanel({
  ownerType,
  ownerId,
  editable = false,
  uploadLabel = '파일 올리기',
  emptyMessage = '첨부된 파일이 없습니다.',
}: Props) {
  const owner = { ownerType, ownerId };
  const { data, isPending, error } = useAttachments(owner);
  const upload = useUploadAttachment(owner);
  const remove = useDeleteAttachment(owner);

  const onPick = async (file: File | null) => {
    if (!file) return;
    try {
      await upload.mutateAsync(file);
      notifications.show({ message: `${file.name} 을(를) 올렸습니다.`, color: 'navy' });
    } catch (e) {
      notifications.show({
        title: '올리지 못했습니다',
        message: e instanceof ApiError ? e.message : '잠시 후 다시 시도해 주세요.',
        color: 'red',
      });
    }
  };

  const onDelete = async (attachment: Attachment) => {
    try {
      await remove.mutateAsync(attachment.id);
      notifications.show({ message: `${attachment.name} 을(를) 지웠습니다.`, color: 'navy' });
    } catch (e) {
      notifications.show({
        title: '지우지 못했습니다',
        message: e instanceof ApiError ? e.message : '잠시 후 다시 시도해 주세요.',
        color: 'red',
      });
    }
  };

  return (
    <Stack gap="sm" data-testid="attachment-panel">
      {editable && (
        <Group justify="space-between" align="center">
          <Text size="sm" c="dimmed">
            {data ? `${data.length}개` : ''}
          </Text>
          <FileButton onChange={onPick} disabled={upload.isPending}>
            {(props) => (
              <Button
                {...props}
                size="xs"
                variant="light"
                leftSection={<IconUpload size={14} />}
                loading={upload.isPending}
              >
                {uploadLabel}
              </Button>
            )}
          </FileButton>
        </Group>
      )}
      {error ? (
        <StateView kind={stateKindOf(error)} compact />
      ) : isPending || !data ? (
        <Stack gap="xs">
          <Skeleton h={44} />
          <Skeleton h={44} />
        </Stack>
      ) : data.length === 0 ? (
        <StateView kind="empty" compact title="첨부 없음" description={emptyMessage} />
      ) : (
        <Stack gap={6} role="list" aria-label="첨부파일">
          {data.map((attachment) => (
            <AttachmentRow
              key={attachment.id}
              attachment={attachment}
              editable={editable}
              onDelete={() => onDelete(attachment)}
              deleting={remove.isPending && remove.variables === attachment.id}
            />
          ))}
        </Stack>
      )}
    </Stack>
  );
}

function AttachmentRow({
  attachment,
  editable,
  onDelete,
  deleting,
}: {
  attachment: Attachment;
  editable: boolean;
  onDelete: () => void;
  deleting: boolean;
}) {
  const [confirming, setConfirming] = useState(false);
  const Icon = iconFor(attachment.contentType, attachment.name);
  return (
    <Group
      gap="sm"
      wrap="nowrap"
      role="listitem"
      px="sm"
      py={6}
      style={{
        border: '1px solid var(--mantine-color-default-border)',
        borderRadius: 'var(--mantine-radius-sm)',
      }}
    >
      <ThemeIcon variant="light" color="navy" size={32} radius="sm">
        <Icon size={18} stroke={1.6} />
      </ThemeIcon>
      <Stack gap={0} style={{ flex: 1, minWidth: 0 }}>
        <Anchor
          href={attachmentDownloadUrl(attachment.id)}
          download={attachment.name}
          size="sm"
          fw={600}
          truncate="end"
        >
          {attachment.name}
        </Anchor>
        <Text size="xs" c="dimmed" truncate="end">
          {formatBytes(attachment.size)} · {attachment.uploadedBy} ·{' '}
          {formatDateTime(attachment.uploadedAt)}
        </Text>
      </Stack>
      {editable && (
        <Popover
          opened={confirming}
          onChange={setConfirming}
          position="left"
          withArrow
          shadow="card"
        >
          <Popover.Target>
            <Tooltip label="지우기">
              <ActionIcon
                variant="subtle"
                color="gray"
                aria-label={`${attachment.name} 지우기`}
                onClick={() => setConfirming((v) => !v)}
                loading={deleting}
              >
                <IconTrash size={16} />
              </ActionIcon>
            </Tooltip>
          </Popover.Target>
          <Popover.Dropdown>
            <Stack gap="xs">
              <Text size="sm">이 파일을 지울까요?</Text>
              <Group gap="xs" justify="flex-end">
                <Button
                  size="compact-xs"
                  variant="subtle"
                  color="gray"
                  onClick={() => setConfirming(false)}
                >
                  취소
                </Button>
                <Button
                  size="compact-xs"
                  color="red"
                  onClick={() => {
                    setConfirming(false);
                    onDelete();
                  }}
                >
                  지우기
                </Button>
              </Group>
            </Stack>
          </Popover.Dropdown>
        </Popover>
      )}
    </Group>
  );
}

function iconFor(contentType: string, name: string) {
  const ext = name.slice(name.lastIndexOf('.') + 1).toLowerCase();
  if (contentType === 'application/pdf' || ext === 'pdf') return IconFileTypePdf;
  if (contentType.startsWith('image/')) return IconPhoto;
  if (['xlsx', 'xls', 'csv'].includes(ext) || contentType.includes('spreadsheet')) {
    return IconFileSpreadsheet;
  }
  if (contentType.startsWith('text/') || ['doc', 'docx', 'hwp', 'hwpx', 'txt'].includes(ext)) {
    return IconFileText;
  }
  return IconFile;
}
