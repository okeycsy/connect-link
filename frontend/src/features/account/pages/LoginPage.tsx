import {
  Alert,
  Anchor,
  Box,
  Button,
  Group,
  Image,
  Paper,
  PasswordInput,
  ScrollArea,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { IconAlertCircle, IconCircleCheck, IconClock } from '@tabler/icons-react';
import { useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';

import { errorCodeOf, fieldErrorsOf } from '../../../api/client';
import kftcLogo from '../../../assets/brand/kftc-logo.png';
import { BrandMark } from '../../../shared/BrandMark';
import { useBusinesses } from '../../organization/hooks';
import { DemoAccountList } from '../components/DemoAccountList';
import { LoginFeatures } from '../components/LoginFeatures';
import { LoginHelpCard } from '../components/LoginHelpCard';
import {
  type CurrentUserResponse,
  homeBusinessSlugOf,
  type LoginRequest,
  safeNextPath,
  useDemoAccounts,
  useLogin,
  useMe,
  useSupportInfo,
} from '../hooks';
import classes from './LoginPage.module.css';

/**
 * 로그인. 결제원 직원과 기관 담당자가 같은 입구를 쓴다("결제원 / 기관" 선택 없음).
 * 회사 이메일과 비밀번호로 세션을 만든다. demo 프로필이면 역할별 데모 계정을 골라 같은 로그인 절차로 들어갈 수 있다.
 * 데스크톱(1366×768 이상)에서는 세로 스크롤 없이 한 화면에 들어온다: 오른쪽 열은 화면 높이에 맞추고 데모 계정 목록만 안에서 넘긴다. SHY 요청(2026-10-25).
 */
export function LoginPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const next = safeNextPath(params.get('next'));
  const withdrawn = params.has('withdrawn');
  const passwordReset = params.has('passwordReset');
  const { data: businesses } = useBusinesses();
  const { data: me } = useMe();
  const { data: demo } = useDemoAccounts();
  const { data: support } = useSupportInfo();
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
  const pendingApproval = errorCode === 'ACCOUNT_PENDING_APPROVAL';
  const showAlert = login.isError && Object.keys(fieldErrorsOf(login.error)).length === 0;

  return (
    <Box className={classes.page}>
      {/* 배경: 시안 B 랜딩과 같은 파스텔 번짐 세 개와 흰 반짝임 */}
      <Box className={classes.blobTopRight} aria-hidden />
      <Box className={classes.blobLeft} aria-hidden />
      <Box className={classes.blobBottomRight} aria-hidden />

      {/* 맨 왼쪽 위 금융결제원 로고(SHY 시안보다 10% 작게) */}
      <Image src={kftcLogo} alt="금융결제원" w="auto" fit="contain" className={classes.kftcLogo} />

      <Box className={classes.panel}>
        <Stack gap="xl" justify="space-between" h="100%">
          <Box aria-hidden />
          <Stack gap="lg" align="flex-start">
            <Box className={classes.tag}>참가기관 · 금융결제원 협업 포털</Box>
            <Title order={1} className={classes.headline}>
              <BrandMark mascot className={classes.headlineLogo} />
            </Title>
            <Text size="xl" c="var(--cl-ink-soft)" maw={460} lh={1.75}>
              결제원과 참가기관 담당자가 서로 찾고, 묻고, 함께 확인하는 업무 공간이에요.
            </Text>
            <Group gap="xs">
              {businesses?.map((b) => (
                <Box key={b.code} className={classes.chip}>
                  {b.label}
                </Box>
              ))}
            </Group>
          </Stack>
          <Text c="dimmed" size="sm">
            샘플 데이터로 동작하는 프로토타입입니다.
          </Text>
        </Stack>
      </Box>

      <Box className={classes.features}>
        <LoginFeatures />
      </Box>

      <Box className={classes.sheet}>
        {/* 카드 뒤 클레이 선 고리와 작은 점, 반짝임 하나 */}
        <Box className={classes.ring} aria-hidden />
        <Box className={classes.ringDot} aria-hidden />
        <Sparkle className={classes.sparkleC} size={40} />
        <Stack gap={12} className={classes.column}>
          {/* 안쪽 여백 32→24: 로그인 화면이 1366×768 에서 스크롤 없이 들어오게(2026-10-25 SHY 요청). */}
          <Paper className={classes.card} p={24}>
            <form onSubmit={form.onSubmit(submit)} noValidate>
              <Stack gap="sm">
                <div>
                  <Title order={2}>로그인</Title>
                  <Text c="dimmed" size="sm">
                    회사 이메일로 로그인합니다.
                  </Text>
                </div>
                {withdrawn && !login.isError && (
                  <Alert
                    variant="light"
                    color="sky"
                    icon={<IconCircleCheck size={18} />}
                    data-testid="login-withdrawn"
                  >
                    탈퇴가 완료되었습니다. 확인 메일을 회사 이메일로 보냈습니다. 같은 이메일로 다시
                    가입할 수 있습니다.
                  </Alert>
                )}
                {passwordReset && !login.isError && (
                  <Alert
                    variant="light"
                    color="sky"
                    icon={<IconCircleCheck size={18} />}
                    data-testid="login-password-reset"
                  >
                    비밀번호를 바꿨습니다. 새 비밀번호로 로그인해 주세요.
                  </Alert>
                )}
                {showAlert && (
                  <Alert
                    variant="light"
                    color={pendingApproval ? 'sky' : 'red'}
                    icon={pendingApproval ? <IconClock size={18} /> : <IconAlertCircle size={18} />}
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
                <Anchor
                  component={Link}
                  to="/password-reset"
                  size="sm"
                  ta="right"
                  mt={-8}
                  data-testid="login-password-reset-link"
                >
                  비밀번호를 잊으셨나요?
                </Anchor>
                <Button
                  type="submit"
                  fullWidth
                  size="md"
                  loading={login.isPending && !pickedEmail}
                  disabled={login.isPending && Boolean(pickedEmail)}
                >
                  로그인
                </Button>
                <Text size="sm" c="dimmed" ta="center">
                  처음 오셨나요?{' '}
                  <Anchor component={Link} to="/sign-up" size="sm" fw={600}>
                    회원가입
                  </Anchor>
                </Text>
              </Stack>
            </form>
          </Paper>

          <LoginHelpCard inquiryEmail={support?.inquiryEmail ?? undefined} />

          {demo && demo.accounts.length > 0 && (
            <Paper className={classes.demoCard} p="md">
              <Text fw={700}>데모 계정으로 들어가기</Text>
              <Text size="xs" c="dimmed" mb="sm">
                시연용 역할별 계정입니다. 비밀번호는 모두{' '}
                <Text span ff="monospace" size="xs" c="var(--cl-ink)">
                  {demo.password}
                </Text>{' '}
                이고, 고르면 같은 로그인 절차를 거칩니다.
              </Text>
              {/* 목록은 화면에 남는 높이만큼만 차지하고 안에서 넘긴다(데스크톱). 휴대폰 폭에서는 300 까지다. */}
              <ScrollArea
                type="auto"
                scrollbars="y"
                scrollbarSize={6}
                offsetScrollbars
                className={classes.demoScroll}
                classNames={{ viewport: classes.demoViewport }}
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
              </ScrollArea>
            </Paper>
          )}
        </Stack>
      </Box>
    </Box>
  );
}

/** 네 갈래 반짝임(흰색). 장식이라 화면 읽기에서 뺀다. */
function Sparkle({ className, size }: { className: string; size: number }) {
  return (
    <svg className={className} width={size} height={size} viewBox="0 0 24 24" aria-hidden>
      <path
        d="M12 0C12.8 7.5 16.5 11.2 24 12C16.5 12.8 12.8 16.5 12 24C11.2 16.5 7.5 12.8 0 12C7.5 11.2 11.2 7.5 12 0Z"
        fill="currentColor"
      />
    </svg>
  );
}
