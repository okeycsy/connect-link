import { Alert, Button, Group, List, Modal, PasswordInput, Stack, Text } from '@mantine/core';
import { useForm } from '@mantine/form';

import { fieldErrorsOf } from '../../../api/client';
import { type CurrentUserResponse, useWithdraw } from '../hooks';

interface Props {
  me: CurrentUserResponse;
  opened: boolean;
  onClose: () => void;
}

/**
 * 탈퇴 확인. 본인 확인으로 지금 비밀번호를 한 번 더 받는다. 비밀번호 검증은 서버가 하고 화면은 서버의 필드 메시지를 그대로 보인다.
 * 성공하면 {@link useWithdraw} 가 로그인 화면으로 보낸다.
 */
export function WithdrawModal({ me, opened, onClose }: Props) {
  const withdraw = useWithdraw();
  const form = useForm({ initialValues: { password: '' } });
  const close = () => {
    withdraw.reset();
    form.reset();
    onClose();
  };
  const generalError =
    withdraw.isError && !fieldErrorsOf(withdraw.error).password
      ? withdraw.error.message
      : undefined;

  return (
    <Modal opened={opened} onClose={close} title="탈퇴" centered>
      <form
        onSubmit={form.onSubmit(({ password }) =>
          withdraw.mutate(password, {
            // 필드 오류는 폼에 넣어 두면 그 칸을 고칠 때 저절로 지워진다.
            onError: (error) => form.setErrors(fieldErrorsOf(error)),
          }),
        )}
        noValidate
      >
        <Stack gap="md">
          <Text size="md">
            {me.organizationName} {me.name} 님의 연결이 계정을 탈퇴합니다.
          </Text>
          <List size="sm" spacing={4} c="var(--cl-ink-soft)">
            <List.Item>이름, 부서, 연락처, 담당 업무 같은 계정 정보와 알림을 지웁니다.</List.Item>
            <List.Item>
              이 계정으로는 다시 로그인할 수 없고, 다른 곳에서 열어 둔 화면도 로그아웃됩니다.
            </List.Item>
            <List.Item>
              같은 회사 이메일로 다시 가입 신청할 수 있습니다. 이때는 가입 승인을 새로 받습니다.
            </List.Item>
          </List>
          <PasswordInput
            label="비밀번호"
            description="본인 확인을 위해 지금 비밀번호를 한 번 더 입력해 주세요."
            autoComplete="current-password"
            withAsterisk
            data-autofocus
            {...form.getInputProps('password')}
          />
          {generalError && (
            <Alert variant="light" color="red" data-testid="withdraw-error">
              {generalError}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={close}>
              취소
            </Button>
            <Button type="submit" color="red" loading={withdraw.isPending}>
              탈퇴하기
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
