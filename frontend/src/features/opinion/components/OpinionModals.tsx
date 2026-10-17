import { Alert, Button, Checkbox, Group, Modal, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import type { OpinionCollationRow } from '../hooks';

interface ConfirmProps {
  opened: boolean;
  onClose: () => void;
  title: string;
  message: string;
  confirmLabel: string;
  /** 위험한 동작(삭제)이면 빨강 채움. */
  danger?: boolean;
  pending: boolean;
  error?: Error | null;
  onConfirm: () => void;
}

/** 되묻는 모달. 회신 요청 시작, 마감, 삭제처럼 사유 없이 확인만 받는 동작에 쓴다. */
export function ConfirmModal({
  opened,
  onClose,
  title,
  message,
  confirmLabel,
  danger = false,
  pending,
  error,
  onConfirm,
}: ConfirmProps) {
  return (
    <Modal opened={opened} onClose={onClose} title={title} centered>
      <Stack gap="md">
        <Text size="md">{message}</Text>
        {error && (
          <Alert variant="light" color="red" data-testid="confirm-error">
            {error.message}
          </Alert>
        )}
        <Group justify="flex-end" gap="xs">
          <Button variant="default" onClick={onClose}>
            닫기
          </Button>
          <Button color={danger ? 'red' : undefined} loading={pending} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

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
  /** 미회신 기관 줄. 회신 담당자가 없는 기관은 고를 수 없다. */
  rows: OpinionCollationRow[];
  pending: boolean;
  error?: Error | null;
  onSend: (organizationIds: number[]) => void;
}

/** 미회신 기관 리마인드. 기본은 담당자가 있는 미회신 기관 전체이고, 일부만 골라 보낼 수 있다. */
export function RemindModal({ opened, onClose, rows, pending, error, onSend }: RemindProps) {
  const sendable = rows.filter((r) => r.recipientNames.length > 0);
  const [selected, setSelected] = useState<string[]>(() =>
    sendable.map((r) => String(r.organizationId)),
  );
  return (
    <Modal opened={opened} onClose={onClose} title="리마인드 메일 보내기" centered size="md">
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
            disabled={selected.length === 0}
            onClick={() => onSend(selected.map(Number))}
          >
            {selected.length}곳에 보내기
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
