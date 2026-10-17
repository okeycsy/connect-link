import { Alert, Button, Group, Modal, Stack, Text } from '@mantine/core';

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

/** 되묻는 모달. 회신 요청 시작, 마감, 삭제처럼 사유 없이 확인만 받는 동작에 쓴다(의견 요청, 일시중단). */
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
