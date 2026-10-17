import { Alert, Button, Group, Modal, Skeleton, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';
import { IconAlertTriangle } from '@tabler/icons-react';

import { fieldErrorsOf } from '../../../../api/client';
import {
  type SignUpApplicationResponse,
  useAccountWithdrawalCheck,
  useWithdrawAccount,
} from '../../hooks';

interface Props {
  application: SignUpApplicationResponse;
  opened: boolean;
  onClose: () => void;
  onDone: () => void;
}

/**
 * 결제원 담당자의 탈퇴 처리 확인. 사유를 꼭 적고, 탈퇴 처리된 담당자에게 가는 메일에 그대로 들어간다. 막는 까닭(예: 후임자 규칙)이 있으면 먼저 보이고
 * 처리 버튼을 막는다. 사유 검증은 서버가 하고 화면은 서버의 필드 메시지를 그대로 보인다.
 */
export function WithdrawAccountModal({ application, opened, onClose, onDone }: Props) {
  const check = useAccountWithdrawalCheck(application.id, opened);
  const withdraw = useWithdrawAccount();
  const form = useForm({ initialValues: { reason: '' } });
  const close = () => {
    withdraw.reset();
    form.reset();
    onClose();
  };
  const generalError =
    withdraw.isError && !fieldErrorsOf(withdraw.error).reason ? withdraw.error.message : undefined;
  const blocked = check.data && !check.data.allowed;

  return (
    <Modal opened={opened} onClose={close} title="탈퇴 처리" centered>
      <form
        onSubmit={form.onSubmit(({ reason }) =>
          withdraw.mutate(
            { id: application.id, reason },
            {
              onSuccess: () => {
                withdraw.reset();
                form.reset();
                onDone();
              },
              // 필드 오류는 폼에 넣어 두면 그 칸을 고칠 때 저절로 지워진다.
              onError: (error) => form.setErrors(fieldErrorsOf(error)),
            },
          ),
        )}
        noValidate
      >
        <Stack gap="md">
          <Text size="md">
            {application.organizationName} {application.name} 님의 계정을 탈퇴 처리합니다. 계정
            정보와 알림이 지워지고 바로 로그아웃됩니다. 계속 담당한다면 같은 회사 이메일로 다시 가입
            신청할 수 있습니다.
          </Text>
          {check.isPending ? (
            <Skeleton h={36} />
          ) : (
            blocked && (
              <Alert
                variant="light"
                color="amber"
                icon={<IconAlertTriangle size={18} />}
                title="지금은 탈퇴 처리할 수 없습니다"
                data-testid="account-withdraw-blockers"
              >
                {check.data.blockers.map((b) => b.message).join(' ')}
              </Alert>
            )
          )}
          <Textarea
            label="탈퇴 처리 사유"
            description="탈퇴 처리된 담당자에게 가는 메일에 그대로 들어갑니다. 500자까지 적을 수 있습니다."
            placeholder="예: 인사이동으로 계좌통합관리 담당이 끝났다고 기관에서 알려 왔습니다."
            autosize
            minRows={3}
            maxLength={500}
            withAsterisk
            data-autofocus
            disabled={blocked}
            {...form.getInputProps('reason')}
          />
          {(generalError || check.isError) && (
            <Alert variant="light" color="red" data-testid="account-withdraw-error">
              {generalError ?? check.error?.message}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={close}>
              취소
            </Button>
            <Button
              type="submit"
              color="red"
              loading={withdraw.isPending}
              disabled={check.isPending || check.isError || blocked}
            >
              탈퇴 처리
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
