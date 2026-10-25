import { Alert, Button, Group, Modal, Stack, Text } from '@mantine/core';

import { fieldErrorsOf } from '../../../api/client';
import { MailPreviewEditor, useMailDraft, type MailDraft } from '../../../shared/MailPreviewEditor';
import { useCampaignReminderPreview } from '../hooks';

interface Props {
  opened: boolean;
  onClose: () => void;
  campaignId: number;
  /** 보낼 기관. 비우면 미완료 기관 전부다. */
  organizationIds: number[];
  /** 위쪽 안내 한 줄(누구에게 보내는지). */
  message: string;
  pending: boolean;
  error?: Error | null;
  onSend: (draft: MailDraft) => void;
}

/** 정기 변경 리마인드 확인 창. 받을 기관과 실제로 나갈 메일 제목·본문을 보여 주고, 고친 뒤 보낸다. */
export function CampaignRemindModal({
  opened,
  onClose,
  campaignId,
  organizationIds,
  message,
  pending,
  error,
  onSend,
}: Props) {
  const preview = useCampaignReminderPreview(campaignId, organizationIds, opened);
  const { draft, setDraft, valid } = useMailDraft(preview.data);
  return (
    <Modal opened={opened} onClose={onClose} title="리마인드 메일 확인" centered size="lg">
      <Stack gap="md">
        <Text size="md">{message}</Text>
        <MailPreviewEditor
          preview={preview.data}
          loading={preview.isPending}
          error={preview.error}
          draft={draft}
          onChange={setDraft}
          fieldErrors={fieldErrorsOf(error)}
        />
        {error && (
          <Alert variant="light" color="red" data-testid="remind-error">
            {error.message}
          </Alert>
        )}
        <Group justify="flex-end" gap="xs">
          <Button variant="default" onClick={onClose}>
            닫기
          </Button>
          <Button
            loading={pending}
            disabled={!preview.data || preview.data.organizations.length === 0 || !valid}
            onClick={() => onSend(draft)}
          >
            {preview.data?.organizations.length ?? 0}곳에 보내기
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
