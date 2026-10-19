import {
  Alert,
  Anchor,
  Badge,
  Button,
  Card,
  Grid,
  Group,
  Skeleton,
  Stack,
  Text,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconAlertTriangle, IconUserOff } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

import { OrgMonogram } from '../../../shared/OrgMonogram';
import { ErrorState } from '../../../shared/state/StateView';
import { MyContactsCard } from '../../contact/components/MyContactsCard';
import { WithdrawModal } from '../components/WithdrawModal';
import { useMe, useWithdrawalCheck } from '../hooks';
import { PageHeader } from '../../../shared/PageHeader';

/**
 * 내 정보. 서버가 아는 로그인 사용자 정보와 연락처 모듈의 내 담당자 정보(업무별)를 보여 주고, 여기서 연락처를 고치거나 "변동 없음"으로
 * 확인하고 탈퇴한다. 담당 업무와 직무는 계정 값이며 레일(업무 영역)의 조회 범위를 정한다.
 */
export function MyInfoPage() {
  const { data: me, isPending, isError, error } = useMe();
  const { data: withdrawal } = useWithdrawalCheck();
  const [withdrawOpened, withdrawModal] = useDisclosure(false);

  if (isError) return <ErrorState error={error} />;

  return (
    <Stack gap="md">
      <PageHeader title="내 정보" description="로그인한 계정과 소속, 연락처를 확인합니다." />

      <Grid gutter="md">
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Card h="100%" data-testid="my-account">
            <Text fw={700} size="lg" mb="sm">
              계정
            </Text>
            {isPending || !me ? (
              <Stack gap="xs">
                <Skeleton h={48} />
                <Skeleton h={20} />
                <Skeleton h={20} />
              </Stack>
            ) : (
              <Stack gap="sm">
                <Group gap="sm" wrap="nowrap">
                  <OrgMonogram name={me.organizationName} type={me.organizationType} size={44} />
                  <div>
                    <Text fw={700} size="xl">
                      {me.name}
                    </Text>
                    <Text size="sm" c="dimmed">
                      {me.organizationName} · {me.organizationTypeLabel}
                    </Text>
                  </div>
                </Group>
                <Field label="회사 이메일" value={me.email} mono />
                <Field label="기관 코드" value={me.organizationCode} mono />
                <Field
                  label="담당 업무"
                  value={
                    <Group gap={6} data-testid="my-businesses">
                      {me.businesses.map((b) => (
                        <Badge key={b.code} color="sky">
                          {b.label}
                        </Badge>
                      ))}
                    </Group>
                  }
                />
                <Field label="직무" value={me.jobRoleLabel} />
                <Field
                  label="권한"
                  value={me.admin ? <Badge color="clay">관리자</Badge> : '담당자'}
                />
                <Field
                  label="지금 접속한 망"
                  value={
                    <Badge color={me.networkZone === 'INTERNAL' ? 'sky' : 'red'}>
                      {me.networkZoneLabel}
                    </Badge>
                  }
                />
              </Stack>
            )}
          </Card>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }}>
          <MyContactsCard />
        </Grid.Col>

        <Grid.Col span={12}>
          <Card data-testid="my-withdraw">
            <Group justify="space-between" align="flex-start" wrap="nowrap" gap="lg">
              <div>
                <Text fw={700} size="lg">
                  탈퇴
                </Text>
                <Text size="sm" c="dimmed" mt={4}>
                  탈퇴하면 이름, 부서, 연락처, 담당 업무 같은 계정 정보가 지워지고 이 계정으로 다시
                  로그인할 수 없습니다. 같은 회사 이메일로 다시 가입할 수 있습니다.
                </Text>
              </div>
              <Button
                color="red"
                variant="light"
                leftSection={<IconUserOff size={16} />}
                disabled={!me || !withdrawal?.allowed}
                onClick={withdrawModal.open}
                style={{ flex: '0 0 auto' }}
              >
                탈퇴하기
              </Button>
            </Group>
            {withdrawal && !withdrawal.allowed && (
              <Alert
                variant="light"
                color="amber"
                mt="md"
                icon={<IconAlertTriangle size={18} />}
                title="지금은 탈퇴할 수 없습니다"
                data-testid="withdraw-blockers"
              >
                <Stack gap={4}>
                  {withdrawal.blockers.map((blocker) => (
                    <Text key={blocker.code} size="sm">
                      {blocker.message}
                      {blocker.link && (
                        <>
                          {' '}
                          <Anchor component={Link} to={blocker.link} size="sm" fw={600}>
                            바로 가기
                          </Anchor>
                        </>
                      )}
                    </Text>
                  ))}
                </Stack>
              </Alert>
            )}
          </Card>
        </Grid.Col>
      </Grid>
      {me && <WithdrawModal me={me} opened={withdrawOpened} onClose={withdrawModal.close} />}
    </Stack>
  );
}

function Field({
  label,
  value,
  mono = false,
}: {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
}) {
  return (
    <Group gap="sm" wrap="nowrap" align="baseline">
      <Text size="sm" c="dimmed" w={120} style={{ flex: '0 0 auto' }}>
        {label}
      </Text>
      {typeof value === 'string' ? (
        // 긴 이메일이 카드 밖으로 넘치지 않게 줄을 바꾼다.
        <Text
          size="md"
          ff={mono ? 'monospace' : undefined}
          style={{ minWidth: 0, overflowWrap: 'anywhere' }}
        >
          {value}
        </Text>
      ) : (
        value
      )}
    </Group>
  );
}
