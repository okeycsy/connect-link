import { Alert, Anchor, Badge, Button, Group, Progress, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconCalendarEvent, IconChecks } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

import { formatDate, formatDateTime } from '../../../shared/format';
import { type BusinessCode, useConfirmOrganization, useCurrentUpdateCampaign } from '../hooks';

interface Props {
  slug: string;
  business: BusinessCode;
  manager: boolean;
  member: boolean;
}

/**
 * 연락처 화면 위쪽의 진행 중인 정기 변경 주간 안내. 기관 담당자는 자기 기관 현행화 상태와 "현행화 완료" 버튼을, 결제원 담당자는
 * 진행률과 현황 화면 바로 가기를 본다. 진행 중인 주간이 없으면 아무것도 그리지 않는다(담당자는 아무 때나 고칠 수 있다).
 */
export function CampaignBanner({ slug, business, manager, member }: Props) {
  const { data } = useCurrentUpdateCampaign(slug);
  const confirm = useConfirmOrganization();
  const campaign = data?.campaign;
  if (!campaign) return null;
  const own = data.ownOrganization;

  return (
    <Alert
      variant="light"
      color={member && own && !own.completed ? 'amber' : 'sky'}
      icon={<IconCalendarEvent size={20} stroke={1.8} />}
      data-testid="campaign-banner"
      title={
        <Group gap="xs">
          <Text fw={700} size="md" component="span">
            {campaign.title}
          </Text>
          <Badge color="sky" size="sm">
            {campaign.statusLabel}
          </Badge>
          <Text size="sm" c="dimmed" component="span">
            {formatDate(campaign.startDate)} ~ {formatDate(campaign.endDate)}
          </Text>
        </Group>
      }
    >
      <Group justify="space-between" align="center" wrap="wrap" gap="sm">
        {member && own && (
          <>
            <Text size="sm">
              {own.completed
                ? `${own.organizationName} 현행화 완료 · ${formatDateTime(own.completedAt)} ${own.completedByName ?? ''}`
                : `정기 변경 주간입니다. ${own.organizationName} 담당자 정보를 확인하고, 바뀐 것은 고친 뒤 "현행화 완료"를 눌러 주세요.`}
            </Text>
            <Button
              variant="light"
              leftSection={<IconChecks size={16} stroke={1.8} />}
              loading={confirm.isPending}
              onClick={() =>
                confirm.mutate(
                  { business },
                  {
                    onSuccess: (r) =>
                      notifications.show({
                        message: `담당자 ${r.confirmedCount}명을 확인하고 현행화 완료로 남겼습니다.`,
                        color: 'clay',
                      }),
                    onError: (e) => notifications.show({ message: e.message, color: 'red' }),
                  },
                )
              }
            >
              {own.completed ? '다시 현행화 완료' : '현행화 완료'}
            </Button>
          </>
        )}
        {manager && (
          <Stack gap={6} style={{ flex: 1, minWidth: 260 }}>
            <Group justify="space-between">
              <Text size="sm">
                완료 {campaign.completedCount} / 대상 {campaign.targetCount}개 기관 · 정보 변경{' '}
                {campaign.changedCount}곳
              </Text>
              <Anchor
                component={Link}
                to={`/${slug}/contacts/update-campaigns?id=${campaign.id}`}
                size="sm"
                fw={600}
              >
                진행 현황 보기
              </Anchor>
            </Group>
            <Progress value={campaign.progressRate} color="clay" size="sm" aria-label="진행률" />
          </Stack>
        )}
      </Group>
    </Alert>
  );
}
