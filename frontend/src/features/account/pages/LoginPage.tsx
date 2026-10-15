import {
  Alert,
  Box,
  Button,
  Group,
  Paper,
  PasswordInput,
  ScrollArea,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { IconAlertCircle, IconClock } from '@tabler/icons-react';
import { useState } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';

import { errorCodeOf, fieldErrorsOf } from '../../../api/client';
import { useBusinesses } from '../../organization/hooks';
import { DemoAccountList } from '../components/DemoAccountList';
import {
  type CurrentUserResponse,
  homeBusinessSlugOf,
  type LoginRequest,
  safeNextPath,
  useDemoAccounts,
  useLogin,
  useMe,
} from '../hooks';
import classes from './LoginPage.module.css';

/**
 * 로그인. 결제원 직원과 기관 담당자가 같은 입구를 쓴다("결제원 / 기관" 선택 없음).
 * 회사 이메일과 비밀번호로 세션을 만든다. demo 프로필이면 역할별 데모 계정을 골라 같은 로그인 절차로 들어갈 수 있다.
 */
export function LoginPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const next = safeNextPath(params.get('next'));
  const { data: businesses } = useBusinesses();
  const { data: me } = useMe();
  const { data: demo } = useDemoAccounts();
  const login = useLogin();
  const [pickedEmail, setPickedEmail] = useState<string>();

  const form = useForm<LoginRequest>({
    initialValues: { email: '', password: '' },
    validate: {
      email: (v) => (v.trim().length > 0 ? null : '회사 이메일을 입력해 주세요.'),
      password: (v) => (v.length > 0 ? null : '비밀번호를 입력해 주세요.'),
    },
  });

  const goHome = (user: CurrentUserResponse) =>
    navigate(next ?? `/${homeBusinessSlugOf(user)}/home`, { replace: true });

  const submit = (body: LoginRequest) =>
    login.mutate(body, {
      onSuccess: goHome,
      onError: (error) => form.setErrors(fieldErrorsOf(error)),
      onSettled: () => setPickedEmail(undefined),
    });

  // 이미 로그인한 상태로 로그인 주소에 오면 바로 들어간다. 로그인 중에는 goHome 이 이동한다.
  if (me && login.isIdle) {
    return <Navigate to={next ?? `/${homeBusinessSlugOf(me)}/home`} replace />;
  }

  const errorCode = errorCodeOf(login.error);
  const showAlert = login.isError && Object.keys(fieldErrorsOf(login.error)).length === 0;

  return (
    <Box className={classes.page}>
      <Box className={classes.panel}>
        <Stack gap="xl" justify="space-between" h="100%">
          <Group gap="sm">
            <Box className={classes.logo}>KFTC</Box>
            <Text c="white" fw={700} size="xl">
              연결고리
            </Text>
          </Group>
          <div>
            <Title order={1} c="white" className={classes.headline}>
              결제원과 기관 담당자를
              <br />
              하나의 고리로 잇습니다.
            </Title>
            <Text c="white" opacity={0.75} mt="sm" size="lg">
              연락처, 의견 요청, 기관 테스트, 일시중단을 한곳에서.
            </Text>
            <Group gap="xs" mt="lg">
              {businesses?.map((b) => (
                <Box key={b.code} className={classes.chip}>
                  {b.label}
                </Box>
              ))}
            </Group>
          </div>
          <Text c="white" opacity={0.5} size="xs">
            샘플 데이터로 동작하는 프로토타입입니다.
          </Text>
        </Stack>
      </Box>

      <Box className={classes.sheet}>
        <Stack gap="md" className={classes.column}>
          <Paper className={classes.card} p="xl" withBorder>
            <form onSubmit={form.onSubmit(submit)} noValidate>
              <Stack gap="md">
                <div>
                  <Title order={2}>로그인</Title>
                  <Text c="dimmed" size="sm">
                    회사 이메일로 로그인합니다.
                  </Text>
                </div>
                {showAlert && (
                  <Alert
                    variant="light"
                    color={errorCode === 'ACCOUNT_PENDING_APPROVAL' ? 'navy' : 'red'}
                    icon={
                      errorCode === 'ACCOUNT_PENDING_APPROVAL' ? (
                        <IconClock size={18} />
                      ) : (
                        <IconAlertCircle size={18} />
                      )
                    }
                    data-testid="login-error"
                  >
                    {login.error?.message}
                  </Alert>
                )}
                <TextInput
                  label="회사 이메일"
                  placeholder="name@company.example.com"
                  autoComplete="username"
                  type="email"
                  {...form.getInputProps('email')}
                />
                <PasswordInput
                  label="비밀번호"
                  placeholder="비밀번호"
                  autoComplete="current-password"
                  {...form.getInputProps('password')}
                />
                <Button
                  type="submit"
                  fullWidth
                  size="md"
                  loading={login.isPending && !pickedEmail}
                  disabled={login.isPending && Boolean(pickedEmail)}
                >
                  로그인
                </Button>
              </Stack>
            </form>
          </Paper>

          {demo && demo.accounts.length > 0 && (
            <Paper className={classes.card} p="md" withBorder>
              <Text fw={700}>데모 계정으로 들어가기</Text>
              <Text size="xs" c="dimmed" mb="sm">
                시연용 역할별 계정입니다. 비밀번호는 모두{' '}
                <Text span ff="monospace" size="xs" c="dark">
                  {demo.password}
                </Text>{' '}
                이고, 고르면 같은 로그인 절차를 거칩니다.
              </Text>
              <ScrollArea.Autosize
                mah={320}
                type="auto"
                scrollbars="y"
                scrollbarSize={6}
                offsetScrollbars
              >
                <DemoAccountList
                  accounts={demo.accounts}
                  pendingEmail={pickedEmail}
                  onPick={(account) => {
                    setPickedEmail(account.email);
                    form.setValues({ email: account.email, password: demo.password });
                    submit({ email: account.email, password: demo.password });
                  }}
                />
              </ScrollArea.Autosize>
            </Paper>
          )}
        </Stack>
      </Box>
    </Box>
  );
}
