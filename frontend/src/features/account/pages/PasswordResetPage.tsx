import { Alert, Button, Card, Group, PasswordInput, Stack, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useQueryClient } from '@tanstack/react-query';
import { IconMailCheck } from '@tabler/icons-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { fieldErrorsOf } from '../../../api/client';
import { isDemoProfile, useServerInfo } from '../../../app/hooks';
import { PageHeader } from '../../../shared/PageHeader';
import { notificationKeys } from '../../notification/hooks';
import { EntryContent, EntryLayout } from '../components/EntryLayout';
import { DemoMailbox } from '../components/signup/DemoMailbox';
import { useSecondsUntil } from '../components/signup/useSecondsUntil';
import { useConfirmPasswordReset, useRequestPasswordReset } from '../hooks';

interface ResetForm {
  email: string;
  code: string;
  newPassword: string;
  newPasswordConfirm: string;
}

/**
 * 비밀번호 찾기(로그인 전). ① 회사 이메일로 숫자 6자리 코드를 받고 ② 코드와 새 비밀번호를 넣는다. 서버는 계정이 있는지 드러내지 않으므로
 * 화면도 "가입한 이메일이면 코드가 갑니다" 로만 안내한다. 바꾸면 로그인 화면(안내 문구)으로 간다.
 */
export function PasswordResetPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: serverInfo } = useServerInfo();
  const demo = isDemoProfile(serverInfo?.profile);
  const request = useRequestPasswordReset();
  const confirm = useConfirmPasswordReset();
  const [sentTo, setSentTo] = useState<string>();
  const [resendAt, setResendAt] = useState<number>();
  const [otherError, setOtherError] = useState<string>();
  const resendIn = useSecondsUntil(resendAt);

  const form = useForm<ResetForm>({
    initialValues: { email: '', code: '', newPassword: '', newPasswordConfirm: '' },
    validate: {
      newPasswordConfirm: (v, values) =>
        v === values.newPassword ? null : '비밀번호가 서로 다릅니다.',
    },
  });

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
      onSuccess: (sent) => {
        setSentTo(sent.email);
        setResendAt(Date.now() + sent.resendAfterSeconds * 1000);
        setOtherError(undefined);
        void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
      },
      onError: showError,
    });
  };

  const submit = (values: ResetForm) =>
    confirm.mutate(
      { email: sentTo ?? values.email, code: values.code.trim(), newPassword: values.newPassword },
      {
        onSuccess: () => navigate('/login?passwordReset=1', { replace: true }),
        onError: showError,
      },
    );

  return (
    <EntryLayout>
      <Stack gap="md">
        <PageHeader
          title="비밀번호 찾기"
          description="가입한 회사 이메일로 인증 코드를 받아 새 비밀번호를 정합니다."
        />
        <EntryContent>
          <Card>
            <form onSubmit={form.onSubmit(submit)} noValidate>
              <Stack gap="md">
                <Group align="flex-end" gap="sm" wrap="nowrap">
                  <TextInput
                    label="회사 이메일"
                    placeholder="name@company.example.com"
                    type="email"
                    autoComplete="username"
                    withAsterisk
                    style={{ flex: 1 }}
                    readOnly={Boolean(sentTo)}
                    {...form.getInputProps('email')}
                  />
                  {sentTo ? (
                    <Button
                      variant="default"
                      onClick={send}
                      loading={request.isPending}
                      disabled={resendIn > 0}
                    >
                      {resendIn > 0 ? `다시 받기 (${resendIn}초)` : '다시 받기'}
                    </Button>
                  ) : (
                    <Button onClick={send} loading={request.isPending}>
                      인증 코드 받기
                    </Button>
                  )}
                </Group>

                {sentTo && (
                  <>
                    <Alert
                      variant="light"
                      color="sky"
                      icon={<IconMailCheck size={18} />}
                      data-testid="password-reset-sent"
                    >
                      가입한 이메일이면 {sentTo} 로 숫자 6자리 인증 코드를 보냈습니다. 메일이 오지
                      않으면 이메일을 다시 확인해 주세요.
                    </Alert>
                    {demo && <DemoMailbox email={sentTo} />}
                    <TextInput
                      label="인증 코드"
                      placeholder="숫자 6자리"
                      inputMode="numeric"
                      maxLength={6}
                      withAsterisk
                      {...form.getInputProps('code')}
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
                      description="같은 비밀번호를 한 번 더"
                      autoComplete="new-password"
                      withAsterisk
                      {...form.getInputProps('newPasswordConfirm')}
                    />
                  </>
                )}

                {otherError && (
                  <Alert variant="light" color="red" data-testid="password-reset-error">
                    {otherError}
                  </Alert>
                )}

                {sentTo && (
                  <Group justify="flex-end">
                    <Button type="submit" loading={confirm.isPending}>
                      비밀번호 바꾸기
                    </Button>
                  </Group>
                )}
                <Text size="xs" c="dimmed">
                  비밀번호를 바꾸면 다른 곳에서 로그인해 있던 화면은 모두 로그아웃됩니다.
                </Text>
              </Stack>
            </form>
          </Card>
        </EntryContent>
      </Stack>
    </EntryLayout>
  );
}
