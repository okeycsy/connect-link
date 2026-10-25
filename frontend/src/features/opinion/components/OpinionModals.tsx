import { Alert, Button, Checkbox, Group, Modal, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import { MailPreviewEditor, useMailDraft, type MailDraft } from '../../../shared/MailPreviewEditor';
import { useOpinionReminderPreview, type OpinionCollationRow } from '../hooks';

interface ReasonProps {
  opened: boolean;
  onClose: () => void;
  title: string;
  message: string;
  label: string;
  placeholder?: string;
  confirmLabel: string;
  danger?: boolean;
  pending: boolean;
  /** 사유를 받아 보낸다. 실패하면 서버의 필드 메시지(reason)를 칸 아래에 보인다. */
  onConfirm: (reason: string, done: { onSuccess: () => void; onError: (e: Error) => void }) => void;
}

/** 사유를 적는 모달. 요청 취소, 회신 수정 요청, 수정 요청 반려에 쓴다. 사유 검증은 서버가 한다. */
export function ReasonModal({
  opened,
  onClose,
  title,
  message,
  label,
  placeholder,
  confirmLabel,
  danger = false,
  pending,
  onConfirm,
}: ReasonProps) {
  const form = useForm({ initialValues: { reason: '' } });
  const [generalError, setGeneralError] = useState<string>();
  const close = () => {
    form.reset();
    setGeneralError(undefined);
    onClose();
  };
  return (
    <Modal opened={opened} onClose={close} title={title} centered>
      <form
        onSubmit={form.onSubmit(({ reason }) =>
          onConfirm(reason, {
            onSuccess: close,
            onError: (error) => {
              const fields = fieldErrorsOf(error);
              form.setErrors(fields);
              setGeneralError(fields.reason ? undefined : error.message);
            },
          }),
        )}
      >
        <Stack gap="md">
          <Text size="md">{message}</Text>
          <Textarea
            label={label}
            placeholder={placeholder}
            autosize
            minRows={3}
            maxLength={500}
            withAsterisk
            data-autofocus
            {...form.getInputProps('reason')}
          />
          {generalError && (
            <Alert variant="light" color="red" data-testid="reason-error">
              {generalError}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={close}>
              닫기
            </Button>
            <Button type="submit" color={danger ? 'red' : undefined} loading={pending}>
              {confirmLabel}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}

interface RemindProps {
  opened: boolean;
  onClose: () => void;
  requestId: number;
  /** 미회신 기관 줄. 회신 담당자가 없는 기관은 고를 수 없다. */
  rows: OpinionCollationRow[];
  pending: boolean;
  error?: Error | null;
  onSend: (organizationIds: number[], draft: MailDraft) => void;
}

/**
 * 미회신 기관 리마인드. 1단계에서 받을 기관을 고르고(기본은 담당자가 있는 미회신 기관 전체), 2단계에서 실제로 나갈 메일 제목·본문을
 * 확인하고 고친 뒤 보낸다.
 */
export function RemindModal({
  opened,
  onClose,
  requestId,
  rows,
  pending,
  error,
  onSend,
}: RemindProps) {
  const sendable = rows.filter((r) => r.recipientNames.length > 0);
  const [selected, setSelected] = useState<string[]>(() =>
    sendable.map((r) => String(r.organizationId)),
  );
  const [step, setStep] = useState<'pick' | 'preview'>('pick');
  const organizationIds = selected.map(Number);
  const preview = useOpinionReminderPreview(
    requestId,
    organizationIds,
    opened && step === 'preview',
  );
  const { draft, setDraft, valid } = useMailDraft(preview.data);
  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={step === 'pick' ? '리마인드 메일 보내기' : '리마인드 메일 확인'}
      centered
      size={step === 'pick' ? 'md' : 'lg'}
    >
      {step === 'pick' ? (
        <Stack gap="md">
          <Text size="md">
            아직 제출하지 않은 기관의 회신 담당자에게 회신 기한을 알리는 메일을 보냅니다.
          </Text>
          <Checkbox.Group value={selected} onChange={setSelected} label="받을 기관">
            <Stack gap={6} mt={6}>
              {rows.map((r) => (
                <Checkbox
                  key={r.organizationId}
                  value={String(r.organizationId)}
                  disabled={r.recipientNames.length === 0}
                  label={
                    r.recipientNames.length === 0
                      ? `${r.organizationName} (회신 담당자 없음)`
                      : `${r.organizationName} · ${r.recipientNames.join(', ')}`
                  }
                />
              ))}
            </Stack>
          </Checkbox.Group>
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={onClose}>
              닫기
            </Button>
            <Button disabled={selected.length === 0} onClick={() => setStep('preview')}>
              다음: 메일 확인
            </Button>
          </Group>
        </Stack>
      ) : (
        <Stack gap="md">
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
          <Group justify="space-between" gap="xs">
            <Button variant="default" onClick={() => setStep('pick')}>
              뒤로
            </Button>
            <Button
              loading={pending}
              disabled={!preview.data || !valid}
              onClick={() => onSend(organizationIds, draft)}
            >
              {preview.data?.organizations.length ?? selected.length}곳에 보내기
            </Button>
          </Group>
        </Stack>
      )}
    </Modal>
  );
}
