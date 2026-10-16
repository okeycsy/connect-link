import { Alert, Button, Group, Modal, Stack, Text, Textarea } from '@mantine/core';
import { useForm } from '@mantine/form';

import { fieldErrorsOf } from '../../../../api/client';
import { type SignUpApplicationResponse, useApproveSignUp, useRejectSignUp } from '../../hooks';

interface Props {
  application: SignUpApplicationResponse;
  opened: boolean;
  onClose: () => void;
  onDone: (result: SignUpApplicationResponse) => void;
}

/** 승인 확인. 승인하면 신청자가 바로 로그인할 수 있고 알림 센터 알림과 승인 메일이 간다. */
export function ApproveSignUpModal({ application, opened, onClose, onDone }: Props) {
  const approve = useApproveSignUp();
  const close = () => {
    approve.reset();
    onClose();
  };
  return (
    <Modal opened={opened} onClose={close} title="가입 승인" centered>
      <Stack gap="md">
        <Text size="md">
          {application.organizationName} {application.name} 님의 가입을 승인합니다. 승인하면 바로
          로그인할 수 있고, 승인 메일과 알림이 갑니다.
        </Text>
        {!application.emailDomainMatches && (
          <Alert variant="light" color="amber">
            이메일 도메인이 기관과 다릅니다. 소속을 확인했는지 한 번 더 봐 주세요.
          </Alert>
        )}
        {approve.isError && (
          <Alert variant="light" color="red" data-testid="approve-error">
            {approve.error.message}
          </Alert>
        )}
        <Group justify="flex-end" gap="xs">
          <Button variant="default" onClick={close}>
            취소
          </Button>
          <Button
            loading={approve.isPending}
            onClick={() =>
              approve.mutate(application.id, {
                onSuccess: (result) => {
                  approve.reset();
                  onDone(result);
                },
              })
            }
          >
            승인하기
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

/**
 * 반려 확인. 사유는 꼭 적고, 신청자에게 가는 반려 메일에 그대로 들어간다. 사유 검증은 서버가 하고 화면은 서버의 필드 메시지를
 * 그대로 보인다.
 */
export function RejectSignUpModal({ application, opened, onClose, onDone }: Props) {
  const reject = useRejectSignUp();
  const form = useForm({ initialValues: { reason: '' } });
  const close = () => {
    reject.reset();
    form.reset();
    onClose();
  };
  const generalError =
    reject.isError && !fieldErrorsOf(reject.error).reason ? reject.error.message : undefined;

  return (
    <Modal opened={opened} onClose={close} title="가입 반려" centered>
      <form
        onSubmit={form.onSubmit(({ reason }) =>
          reject.mutate(
            { id: application.id, reason },
            {
              onSuccess: (result) => {
                reject.reset();
                form.reset();
                onDone(result);
              },
              // 필드 오류는 폼에 넣어 두면 그 칸을 고칠 때 저절로 지워진다.
              onError: (error) => form.setErrors(fieldErrorsOf(error)),
            },
          ),
        )}
      >
        <Stack gap="md">
          <Text size="md">
            {application.organizationName} {application.name} 님의 가입 신청을 반려합니다. 신청자는
            사유를 메일로 받고, 내용을 고쳐 다시 신청할 수 있습니다.
          </Text>
          <Textarea
            label="반려 사유"
            description="신청자에게 가는 반려 메일에 그대로 들어갑니다. 500자까지 적을 수 있습니다."
            placeholder="예: 기관 담당자 명단에서 확인되지 않습니다. 부서장 확인 뒤 다시 신청해 주세요."
            autosize
            minRows={3}
            maxLength={500}
            withAsterisk
            data-autofocus
            {...form.getInputProps('reason')}
          />
          {generalError && (
            <Alert variant="light" color="red" data-testid="reject-error">
              {generalError}
            </Alert>
          )}
          <Group justify="flex-end" gap="xs">
            <Button variant="default" onClick={close}>
              취소
            </Button>
            <Button type="submit" color="red" loading={reject.isPending}>
              반려하기
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
