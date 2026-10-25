import {
  Alert,
  Anchor,
  Group,
  Paper,
  Skeleton,
  Stack,
  Text,
  TextInput,
  Textarea,
} from '@mantine/core';
import { useEffect, useState } from 'react';

import type { components } from '../api/schema';

export type MailPreview = components['schemas']['MailPreviewResponse'];

/** 보낼 메일 제목과 본문. */
export interface MailDraft {
  subject: string;
  body: string;
}

/**
 * 미리보기를 받으면 기본 문구로 채우는 메일 초안. 미리보기가 바뀌면(받을 기관을 다시 고르면) 기본 문구로 다시 채운다.
 */
export function useMailDraft(preview: MailPreview | undefined) {
  const [draft, setDraft] = useState<MailDraft>({ subject: '', body: '' });
  useEffect(() => {
    if (preview) {
      setDraft({ subject: preview.subject, body: preview.body });
    }
  }, [preview]);
  const valid =
    draft.subject.trim().length > 0 &&
    draft.body.trim().length > 0 &&
    (!preview ||
      (draft.subject.length <= preview.subjectMaxLength &&
        draft.body.length <= preview.bodyMaxLength));
  return { draft, setDraft, valid };
}

interface Props {
  preview: MailPreview | undefined;
  loading: boolean;
  /** 미리보기를 불러오지 못한 오류. */
  error?: Error | null;
  draft: MailDraft;
  onChange: (draft: MailDraft) => void;
  /** 보내기에서 돌아온 서버의 필드 오류(subject, body). */
  fieldErrors?: Record<string, string>;
}

/**
 * 보내기 전 메일 미리보기. 받을 기관과 실제로 나갈 제목·본문을 보여 주고, 보내는 사람이 고칠 수 있다. 고친 문구는 받는 사람 모두에게 같게
 * 간다(리마인드 메일: 의견 요청, 연락처 정기 변경).
 */
export function MailPreviewEditor({
  preview,
  loading,
  error,
  draft,
  onChange,
  fieldErrors,
}: Props) {
  if (error) {
    return (
      <Alert variant="light" color="red" data-testid="mail-preview-error">
        {error.message}
      </Alert>
    );
  }
  if (loading || !preview) {
    return <Skeleton h={260} />;
  }
  const changed = draft.subject !== preview.subject || draft.body !== preview.body;
  return (
    <Stack gap="sm" data-testid="mail-preview">
      <Paper withBorder radius="md" p="sm" bg="var(--mantine-color-gray-0)">
        <Text size="sm" fw={600}>
          받는 곳 · {preview.organizations.length}개 기관 {preview.recipientCount}명
        </Text>
        <Text size="sm" c="dimmed" mt={2}>
          {preview.organizations
            .map((o) => `${o.organizationName} ${o.recipientCount}명`)
            .join(', ') || '받을 사람이 없습니다.'}
        </Text>
        {preview.skippedOrganizationNames.length > 0 && (
          <Text size="xs" c="orange.8" mt={4}>
            받을 사람이 없어 건너뜀: {preview.skippedOrganizationNames.join(', ')}
          </Text>
        )}
      </Paper>
      <TextInput
        label="제목"
        value={draft.subject}
        onChange={(e) => onChange({ ...draft, subject: e.currentTarget.value })}
        error={
          fieldErrors?.subject ??
          (draft.subject.trim() === '' ? '제목을 입력해 주세요.' : undefined)
        }
        maxLength={preview.subjectMaxLength}
      />
      <Textarea
        label="본문"
        value={draft.body}
        onChange={(e) => onChange({ ...draft, body: e.currentTarget.value })}
        error={
          fieldErrors?.body ?? (draft.body.trim() === '' ? '본문을 입력해 주세요.' : undefined)
        }
        autosize
        minRows={5}
        maxRows={12}
        maxLength={preview.bodyMaxLength}
        description={`${draft.body.length} / ${preview.bodyMaxLength}자`}
      />
      <Group justify="space-between" gap="xs">
        <Text size="xs" c="dimmed">
          고친 문구가 받는 사람 모두에게 그대로 갑니다.
        </Text>
        {changed && (
          <Anchor
            component="button"
            type="button"
            size="xs"
            onClick={() => onChange({ subject: preview.subject, body: preview.body })}
          >
            원래 문구로
          </Anchor>
        )}
      </Group>
    </Stack>
  );
}
