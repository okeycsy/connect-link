import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Group,
  Paper,
  Skeleton,
  Stack,
  Text,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState, StateView } from '../../../shared/state/StateView';
import { useMe } from '../../account/hooks';
import {
  type TestPlanRecipient,
  canManageTestPlans,
  useTestPlan,
  useTestPlanRecipients,
  useUpdateTestPlanRecipients,
} from '../hooks';

/**
 * 결과 등록 알림 설정(테스트별). 기관이 결과를 등록하면 매터모스트 알림을 받을 결제원 담당자를 고른다. 후보는 이 업무를 맡은 사용 중 결제원
 * 계정이다. 운영·개발·분석·사업부서 담당자 누구나 고를 수 있다.
 */
export function TestPlanRecipientsPage() {
  const { business: slug = '', id } = useParams<{ business: string; id: string }>();
  const planId = Number(id);
  const { data: me } = useMe();
  const manage = canManageTestPlans(me, slug);
  const plan = useTestPlan(Number.isInteger(planId) ? planId : null);
  const recipients = useTestPlanRecipients(manage && Number.isInteger(planId) ? planId : null);

  if (me && !manage) {
    return (
      <Stack gap="md">
        <PageHeader title="결과 등록 알림 설정" />
        <StateView
          kind="forbidden"
          title="알림 대상을 바꿀 수 없습니다"
          description="이 업무를 맡은 결제원 담당자만 알림 대상을 정합니다."
        />
      </Stack>
    );
  }
  const error = plan.error ?? recipients.error;
  if (error) {
    return (
      <Stack gap="md">
        <PageHeader title="결과 등록 알림 설정" />
        <ErrorState error={error} homeLink={`/${slug}/home`} />
      </Stack>
    );
  }
  if (!plan.data || !recipients.data) {
    return (
      <Stack gap="md">
        <PageHeader title="결과 등록 알림 설정" />
        <Skeleton h={280} />
      </Stack>
    );
  }
  return (
    <RecipientsEditor
      slug={slug}
      planId={plan.data.id}
      planTitle={plan.data.title}
      rows={recipients.data}
    />
  );
}

function RecipientsEditor({
  slug,
  planId,
  planTitle,
  rows,
}: {
  slug: string;
  planId: number;
  planTitle: string;
  rows: TestPlanRecipient[];
}) {
  const navigate = useNavigate();
  const save = useUpdateTestPlanRecipients(planId);
  const candidates = rows.filter((r) => r.candidate && r.accountId != null);
  // 후보가 아닌 사람(업무를 더 맡지 않거나 탈퇴한 계정)은 저장하면 빠진다.
  const leaving = rows.filter((r) => !r.candidate);
  const [selected, setSelected] = useState<Set<number>>(
    () => new Set(candidates.filter((r) => r.selected).map((r) => r.accountId!)),
  );
  const toggle = (accountId: number, checked: boolean) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (checked) next.add(accountId);
      else next.delete(accountId);
      return next;
    });
  const back = `/${slug}/tests/${planId}`;

  return (
    <Stack gap="md">
      <PageHeader
        title="결과 등록 알림 설정"
        description={`${planTitle} · 기관이 결과를 등록하면 고른 결제원 담당자에게 매터모스트 알림이 갑니다.`}
        actions={
          <>
            <Button variant="default" component={Link} to={back}>
              취소
            </Button>
            <Button
              loading={save.isPending}
              onClick={() =>
                save.mutate([...selected], {
                  onSuccess: () => {
                    notifications.show({ message: '알림 대상을 저장했습니다.', color: 'sky' });
                    navigate(`${back}?tab=content`);
                  },
                })
              }
            >
              저장
            </Button>
          </>
        }
      />

      {save.isError && (
        <Alert variant="light" color="red">
          {save.error.message}
        </Alert>
      )}

      <Card>
        <Stack gap="sm">
          <Group justify="space-between">
            <div>
              <Text fw={700} size="xl">
                알림 받을 결제원 담당자
              </Text>
              <Text size="sm" c="dimmed">
                이 테스트에만 적용됩니다. 아무도 고르지 않으면 결과 등록 알림이 가지 않습니다.
              </Text>
            </div>
            <Text size="sm" c="dimmed" data-testid="recipient-count">
              {selected.size}명 선택
            </Text>
          </Group>
          {candidates.length === 0 ? (
            <StateView
              kind="empty"
              compact
              description="이 업무를 맡은 결제원 담당자가 없습니다."
            />
          ) : (
            candidates.map((r) => (
              <Paper
                key={r.accountId}
                p="sm"
                radius="lg"
                bg={selected.has(r.accountId!) ? 'clay.0' : 'gray.1'}
              >
                <Checkbox
                  checked={selected.has(r.accountId!)}
                  onChange={(e) => toggle(r.accountId!, e.currentTarget.checked)}
                  aria-label={`${r.name} 알림 받기`}
                  label={
                    <Group gap="xs" wrap="wrap">
                      <Text fw={600}>{r.name}</Text>
                      {r.jobRoleLabel && (
                        <Badge size="xs" color="gray" variant="outline">
                          {r.jobRoleLabel}
                        </Badge>
                      )}
                      <Text size="sm" c="dimmed">
                        {[r.department, r.team].filter(Boolean).join(' ')} · {r.email}
                      </Text>
                    </Group>
                  }
                />
              </Paper>
            ))
          )}
          {leaving.length > 0 && (
            <Alert variant="light" color="amber">
              {leaving.map((r) => r.name).join(', ')} 님은 이 업무를 더 맡지 않아 저장하면 알림
              대상에서 빠집니다.
            </Alert>
          )}
        </Stack>
      </Card>
    </Stack>
  );
}
