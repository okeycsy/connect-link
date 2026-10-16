import { Badge, Button, Card, Grid, Group, Skeleton, Stack, Text } from '@mantine/core';
import { IconUserCheck, IconUserOff } from '@tabler/icons-react';

import { OrgMonogram } from '../../../shared/OrgMonogram';
import { ErrorState } from '../../../shared/state/StateView';
import { useMe } from '../hooks';
import { PageHeader } from '../../../shared/PageHeader';

/**
 * 내 정보. 지금은 읽기 전용(SHY 결정 2026-10-15): 서버가 아는 로그인 사용자 정보를 보여 주고, 연락처 수정·"변동 없음" 확인·탈퇴 자리는
 * 계정·연락처 모듈이 붙을 때 동작을 연결한다. 담당 업무와 직무는 계정 값이며 레일(업무 영역)의 조회 범위를 정한다.
 */
export function MyInfoPage() {
  const { data: me, isPending, isError, error } = useMe();

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
          <Card h="100%" data-testid="my-contact">
            <Group justify="space-between" mb="sm">
              <Text fw={700} size="lg">
                연락처
              </Text>
              <Badge variant="outline" color="gray">
                준비 중 · 연락처 모듈
              </Badge>
            </Group>
            <Text size="sm" c="dimmed" mb="md">
              부서·팀·직급, 내선과 휴대폰은 가입 때 적은 값이 들어옵니다. 고치면 최근 확인일이
              갱신되고, 바뀐 것이 없으면 "변동 없음"으로 확인합니다.
            </Text>
            <Stack gap="sm">
              <Field label="세부업무" value="-" />
              <Field label="부서 · 팀 · 직급" value="-" />
              <Field label="내선 · 휴대폰" value="-" />
              <Field label="최근 확인일" value="-" />
            </Stack>
            <Group justify="flex-end" mt="md" gap="xs">
              <Button variant="light" leftSection={<IconUserCheck size={16} />} disabled>
                변동 없음
              </Button>
              <Button disabled>연락처 수정</Button>
            </Group>
          </Card>
        </Grid.Col>

        <Grid.Col span={12}>
          <Card data-testid="my-withdraw">
            <Group justify="space-between" align="flex-start">
              <div>
                <Text fw={700} size="lg">
                  탈퇴
                </Text>
                <Text size="sm" c="dimmed" mt={4}>
                  기관에 담당자가 혼자면 후임자를 먼저 적어야 탈퇴할 수 있습니다. 계정 모듈이 붙으면
                  여기서 진행합니다.
                </Text>
              </div>
              <Button
                variant="subtle"
                color="gray"
                leftSection={<IconUserOff size={16} />}
                disabled
              >
                탈퇴 (계정 모듈 후)
              </Button>
            </Group>
          </Card>
        </Grid.Col>
      </Grid>
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
        <Text size="md" ff={mono ? 'monospace' : undefined}>
          {value}
        </Text>
      ) : (
        value
      )}
    </Group>
  );
}
