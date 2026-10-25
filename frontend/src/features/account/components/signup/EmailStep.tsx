import { Alert, Box, Button, Group, Stack, Text, TextInput } from '@mantine/core';
import { useQueryClient } from '@tanstack/react-query';
import { useInterval } from '@mantine/hooks';
import { IconCircleCheck, IconMailCheck } from '@tabler/icons-react';
import { useEffect, useState } from 'react';

import { errorCodeOf, fieldErrorsOf } from '../../../../api/client';
import { formatDateTime } from '../../../../shared/format';
import {
  type EmailVerificationConfirmResponse,
  type EmailVerificationResponse,
  useConfirmVerification,
  useRequestVerification,
} from '../../hooks';
import { notificationKeys } from '../../../notification/hooks';
import { DemoMailbox } from './DemoMailbox';
import type { SignUpForm } from './signUpForm';

/** 보낸 인증 코드. 다시 받기 시각은 서버가 준 남은 초로 이 브라우저 시계에 맞춰 둔다. */
export interface SentCode extends EmailVerificationResponse {
  /** 다시 받을 수 있는 시각(Date.now() 기준 밀리초). */
  resendAt: number;
}

interface Props {
  form: SignUpForm;
  sent: SentCode | undefined;
  verified: EmailVerificationConfirmResponse | undefined;
  /** demo 프로필이면 받은 메일함(인증 코드 메일)을 함께 보여 준다. */
  demo: boolean;
  onSent: (sent: SentCode) => void;
  onVerified: (verified: EmailVerificationConfirmResponse) => void;
  onReset: () => void;
}

/** 가입 ①: 회사 이메일로 숫자 6자리 코드를 받아 확인한다. 확인하면 이메일은 바꿀 수 없고, 바꾸려면 처음부터 다시 받는다. */
export function EmailStep({ form, sent, verified, demo, onSent, onVerified, onReset }: Props) {
  const queryClient = useQueryClient();
  const request = useRequestVerification();
  const confirm = useConfirmVerification();
  const [otherError, setOtherError] = useState<string>();
  const resendIn = useSecondsUntil(sent?.resendAt);

  const showError = (error: unknown) => {
    const fields = fieldErrorsOf(error);
    if (Object.keys(fields).length > 0) {
      form.setErrors(fields);
      setOtherError(undefined);
    } else {
      setOtherError(error instanceof Error ? error.message : '요청에 실패했습니다.');
    }
  };

  const send = () => {
    const email = form.values.email.trim();
    if (!email) {
      form.setFieldError('email', '회사 이메일을 입력해 주세요.');
      return;
    }
    request.mutate(email, {
      onSuccess: (result) => {
        form.setFieldValue('code', '');
        form.clearFieldError('email');
        form.clearFieldError('code');
        setOtherError(undefined);
        // 데모 메일함이 새 코드 메일을 다시 읽게 한다.
        void queryClient.invalidateQueries({
          queryKey: notificationKeys.demoMailbox(result.email),
        });
        onSent({ ...result, resendAt: Date.now() + result.resendAfterSeconds * 1000 });
      },
      onError: showError,
    });
  };

  const check = () => {
    if (!sent) return;
    const code = form.values.code.trim();
    if (!code) {
      form.setFieldError('code', '인증 코드를 입력해 주세요.');
      return;
    }
    confirm.mutate(
      { email: sent.email, code },
      {
        onSuccess: (result) => {
          form.clearFieldError('code');
          setOtherError(undefined);
          onVerified(result);
        },
        onError: (error) => {
          // 서버에 보낸 코드 기록이 없으면(데모 서버가 다시 시작돼 메모리 DB 가 비었을 때 등) 이 코드로는 끝낼 수 없다.
          // 재발송 간격을 기다리지 않고 바로 다시 받게 처음 칸으로 되돌리고 서버 메시지는 이메일 칸에 보여 준다.
          if (errorCodeOf(error) === 'VERIFICATION_NOT_REQUESTED') {
            const message = fieldErrorsOf(error).code;
            onReset();
            form.setFieldError('email', message ?? '인증 코드를 다시 받아 주세요.');
            return;
          }
          showError(error);
        },
      },
    );
  };

  // Enter 는 단계 넘김 대신 지금 칸의 동작(코드 받기, 확인)을 한다.
  const onEnter = (action: () => void) => (event: React.KeyboardEvent) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      action();
    }
  };

  return (
    <Stack gap="md">
      <Text size="sm" c="dimmed">
        회사 이메일로 본인을 확인합니다. 가입 승인과 업무 알림도 이 주소로 갑니다.
      </Text>
      <TextInput
        label="회사 이메일"
        placeholder="name@company.example.com"
        type="email"
        autoComplete="email"
        withAsterisk
        readOnly={Boolean(sent)}
        onKeyDown={onEnter(send)}
        rightSectionWidth={sent ? 40 : 128}
        rightSectionPointerEvents="all"
        rightSection={
          verified ? (
            <IconCircleCheck size={18} aria-label="인증됨" color="var(--mantine-color-sky-6)" />
          ) : sent ? undefined : (
            <Button size="compact-sm" variant="light" loading={request.isPending} onClick={send}>
              인증 코드 받기
            </Button>
          )
        }
        {...form.getInputProps('email')}
      />

      {sent && !verified && (
        <Box>
          <TextInput
            label="인증 코드"
            placeholder="숫자 6자리"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            withAsterisk
            description={`${sent.email} 로 보낸 코드를 ${formatDateTime(sent.expiresAt)} 까지 입력해 주세요.`}
            onKeyDown={onEnter(check)}
            rightSectionWidth={72}
            rightSectionPointerEvents="all"
            rightSection={
              <Button size="compact-sm" variant="light" loading={confirm.isPending} onClick={check}>
                확인
              </Button>
            }
            {...form.getInputProps('code')}
          />
          <Group gap="xs" mt="xs">
            <Button
              variant="subtle"
              size="compact-sm"
              disabled={resendIn > 0}
              loading={request.isPending}
              onClick={send}
            >
              {resendIn > 0 ? `코드 다시 받기 (${resendIn}초)` : '코드 다시 받기'}
            </Button>
            <Button variant="subtle" color="gray" size="compact-sm" onClick={onReset}>
              다른 이메일로 하기
            </Button>
          </Group>
        </Box>
      )}

      {verified && (
        <Alert
          variant="light"
          color="sky"
          icon={<IconMailCheck size={18} />}
          title="이메일 인증을 마쳤습니다"
          data-testid="email-verified"
        >
          <Text size="sm">
            {formatDateTime(verified.tokenExpiresAt)} 까지 가입 신청을 마쳐 주세요. 지나면 이메일
            인증을 다시 합니다.
          </Text>
          <Button variant="subtle" color="gray" size="compact-sm" mt={6} onClick={onReset}>
            다른 이메일로 하기
          </Button>
        </Alert>
      )}

      {otherError && (
        <Alert variant="light" color="red">
          {otherError}
        </Alert>
      )}

      {demo && sent && !verified && <DemoMailbox email={sent.email} />}
    </Stack>
  );
}

/** 주어진 시각(밀리초)까지 남은 초(올림). 지났거나 없으면 0. 1초마다 다시 센다. */
function useSecondsUntil(target: number | undefined): number {
  const [now, setNow] = useState(() => Date.now());
  const interval = useInterval(() => setNow(Date.now()), 1000);
  useEffect(() => {
    if (!target) return;
    setNow(Date.now());
    interval.start();
    return interval.stop;
    // interval 객체는 렌더마다 새로 만들어져 의존성에서 뺀다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target]);
  if (!target) return 0;
  return Math.max(0, Math.ceil((target - now) / 1000));
}
