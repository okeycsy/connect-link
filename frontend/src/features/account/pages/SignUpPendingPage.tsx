import { Badge, Button, Card, Group, Stack, Text, ThemeIcon } from '@mantine/core';
import { IconClockHour4 } from '@tabler/icons-react';
import { Link, useLocation } from 'react-router-dom';

import { formatDateTime } from '../../../shared/format';
import { PageHeader } from '../../../shared/PageHeader';
import { EntryContent, EntryLayout } from '../components/EntryLayout';
import type { SignUpResponse } from '../hooks';

/**
 * 승인 대기. 가입 신청 직후에 보이고, 신청 내용(가입 화면이 넘겨준 응답)을 요약한다. 새로 고침하면 요약 없이 안내만 남는다.
 * 승인 전에는 로그인할 수 없고, 로그인 화면은 같은 계정에 "가입 승인 전" 안내를 띄운다.
 */
export function SignUpPendingPage() {
  const location = useLocation();
  const applied = location.state as SignUpResponse | null;

  return (
    <EntryLayout>
      <Stack gap="md">
        <PageHeader
          title="승인 대기"
          description="가입 신청을 받았습니다. 승인되면 회사 이메일로 알려 드립니다."
          status={<Badge color="sky">{applied?.statusLabel ?? '승인 대기'}</Badge>}
        />
        <EntryContent>
          <Card data-testid="sign-up-pending">
            <Stack gap="md">
              <Group gap="md" wrap="nowrap" align="flex-start">
                <ThemeIcon variant="light" color="clay" size={44} radius="md">
                  <IconClockHour4 size={24} stroke={1.7} />
                </ThemeIcon>
                <div>
                  <Text fw={700} size="xl">
                    결제원 담당자의 승인을 기다리고 있습니다
                  </Text>
                  <Text size="sm" c="dimmed" mt={4}>
                    고른 업무를 맡은 결제원 담당자가 소속과 회사 이메일을 확인한 뒤 승인합니다. 승인
                    전에는 로그인할 수 없습니다.
                  </Text>
                </div>
              </Group>

              {applied && (
                <Stack gap={8} data-testid="sign-up-applied">
                  <Field label="회사 이메일" value={applied.email} mono />
                  <Field label="이름" value={applied.name} />
                  <Field
                    label="소속 기관"
                    value={`${applied.organizationName} · ${applied.organizationTypeLabel}`}
                  />
                  <Field
                    label="담당 업무"
                    value={
                      <Group gap={6}>
                        {applied.businesses.map((b) => (
                          <Badge key={b.code} color="sky">
                            {b.label}
                          </Badge>
                        ))}
                      </Group>
                    }
                  />
                  <Field label="직무" value={applied.jobRoleLabel} />
                  <Field label="신청 일시" value={formatDateTime(applied.appliedAt)} />
                </Stack>
              )}

              <Group justify="flex-end">
                <Button component={Link} to="/login">
                  로그인 화면으로
                </Button>
              </Group>
            </Stack>
          </Card>
        </EntryContent>
      </Stack>
    </EntryLayout>
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
      <Text size="sm" c="dimmed" w={96} flex="0 0 auto">
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
