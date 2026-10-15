import {
  Anchor,
  Box,
  Button,
  Checkbox,
  Group,
  Paper,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';

import { useBusinesses } from '../../organization/hooks';
import classes from './LoginPage.module.css';

/**
 * 로그인. 결제원 직원과 기관 담당자가 같은 입구를 쓴다("결제원 / 기관" 선택 없음).
 * 실제 인증 API 는 account 모듈과 함께 다음 단계에서 붙는다.
 */
export function LoginPage() {
  const { data: businesses } = useBusinesses();
  const form = useForm({
    initialValues: { email: '', password: '', remember: true },
    validate: {
      email: (v) => (/^\S+@\S+\.\S+$/.test(v) ? null : '회사 이메일을 입력해 주세요.'),
      password: (v) => (v.length > 0 ? null : '비밀번호를 입력해 주세요.'),
    },
  });

  const handleSubmit = form.onSubmit(() => {
    notifications.show({
      title: '아직 연결 전입니다',
      message: '로그인 API 는 계정 모듈과 함께 다음 작업에서 붙습니다.',
      color: 'navy',
    });
  });

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
        <Paper className={classes.card} p="xl" withBorder>
          <form onSubmit={handleSubmit}>
            <Stack gap="md">
              <div>
                <Title order={2}>로그인</Title>
                <Text c="dimmed" size="sm">
                  회사 이메일로 로그인합니다.
                </Text>
              </div>
              <TextInput
                label="회사 이메일"
                placeholder="name@company.example.com"
                autoComplete="username"
                {...form.getInputProps('email')}
              />
              <PasswordInput
                label="비밀번호"
                placeholder="비밀번호"
                autoComplete="current-password"
                {...form.getInputProps('password')}
              />
              <Group justify="space-between">
                <Checkbox
                  label="로그인 유지"
                  {...form.getInputProps('remember', { type: 'checkbox' })}
                />
                <Anchor size="sm" href="#">
                  비밀번호를 잊으셨나요?
                </Anchor>
              </Group>
              <Button type="submit" fullWidth size="md">
                로그인
              </Button>
              <Text size="sm" ta="center" c="dimmed">
                아직 계정이 없으신가요? <Anchor href="#">회원가입</Anchor>
              </Text>
            </Stack>
          </form>
        </Paper>
      </Box>
    </Box>
  );
}
