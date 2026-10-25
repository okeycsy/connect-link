import { Alert, Button, Group, Modal, PasswordInput, Stack, Text } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';

import { fieldErrorsOf } from '../../../api/client';
import { useChangePassword } from '../hooks';

interface Props {
  opened: boolean;
  onClose: () => void;
}

interface ChangeForm {
  currentPassword: string;
  newPassword: string;
  newPasswordConfirm: string;
}

/**
 * 내 정보 > 비밀번호 변경. 지금 비밀번호로 본인을 확인한다. 비밀번호 규칙과 맞는지는 서버가 보고 화면은 필드 메시지를 그대로 보인다.
 * 바꾸면 이 화면은 그대로이고 다른 곳에서 로그인한 화면은 끝난다.
 */
export function PasswordChangeModal({ opened, onClose }: Props) {
  const change = useChangePassword();
  const form = useForm<ChangeForm>({
    initialValues: { currentPassword: '', newPassword: '', newPasswordConfirm: '' },
    validate: {
      newPasswordConfirm: (v, values) =>
        v === values.newPassword ? null : '비밀번호가 서로 다릅니다.',
    },
  });
  const close = () => {
    change.reset();
    form.reset();
    onClose();
  };
  const generalError =
    change.isError && Object.keys(fieldErrorsOf(change.error)).length === 0
      ? change.error.message
      : undefined;

  return (
    <Modal opened={opened} onClose={close} title="비밀번호 변경" centered>
      <form
        onSubmit={form.onSubmit(({ currentPassword, newPassword }) =>
          change.mutate(
            { currentPassword, newPassword },
            {
              onSuccess: () => {
                notifications.show({
                  color: 'sky',
                  message: '비밀번호를 바꿨습니다. 다른 곳에서 로그인한 화면은 로그아웃됩니다.',
                });
                close();
              },
              onError: (error) => form.setErrors(fieldErrorsOf(error)),
            },
          ),
        )}
        noValidate
      >
        <Stack gap="md">
          <Text size="sm" c="var(--cl-ink-soft)">
            바꾸면 확인 메일이 회사 이메일로 가고, 다른 곳에서 로그인해 있던 화면은 다시 로그인해야
            합니다.
          </Text>
          <PasswordInput
            label="지금 비밀번호"
            autoComplete="current-password"
            withAsterisk
            data-autofocus
            {...form.getInputProps('currentPassword')}
          />
          <PasswordInput
            label="새 비밀번호"
            description="영문, 숫자, 특수문자를 모두 넣어 8~64자"
            autoComplete="new-password"
            withAsterisk
            {...form.getInputProps('newPassword')}
          />
          <PasswordInput
            label="새 비밀번호 확인"
            autoComplete="new-password"
            withAsterisk
            {...form.getInputProps('newPasswordConfirm')}
          />
          {generalError && (
            <Alert variant="light" color="red">
              {generalError}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={close}>
              취소
            </Button>
            <Button type="submit" loading={change.isPending}>
              바꾸기
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
