import { Badge, Card, Group, SimpleGrid, Stack, Text, UnstyledButton } from '@mantine/core';
import { IconChevronRight } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

import { useMe, useSignUpCounts } from '../../features/account/hooks';
import { useCurrentBusiness } from '../../features/organization/hooks';
import { ADMIN_MENU, type AdminMenuItem } from '../navigation';
import classes from './AdminHubPage.module.css';
import { PageHeader } from '../../shared/PageHeader';

/**
 * 관리 그룹의 첫 화면. 항목 카드를 늘어놓고, 아직 화면이 없는 자리는 준비 중으로 표시한다. 관리자 전용 항목은 결제원 담당자에게
 * 보이지 않는다.
 */
export function AdminHubPage() {
  const business = useCurrentBusiness();
  const { data: me } = useMe();
  const { data: counts } = useSignUpCounts(business?.slug);
  const items = ADMIN_MENU.filter((item) => !item.adminOnly || me?.admin);
  const badgeOf = (item: AdminMenuItem) =>
    item.key === 'signup-approval' && counts ? (
      <Badge color={counts.pending > 0 ? 'amber' : 'sky'} size="sm">
        대기 {counts.pending}건
      </Badge>
    ) : null;
  return (
    <Stack gap="md">
      <PageHeader
        title="관리"
        description={
          <>
            {business?.label ?? '업무 영역'}의 가입 승인과 운영 설정. 결제원 담당자와 관리자만 보고,
            기준정보는 관리자만 고칩니다.
          </>
        }
      />
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" data-testid="admin-menu">
        {items.map((item) =>
          item.path ? (
            <UnstyledButton
              key={item.key}
              component={Link}
              to={`/${business?.slug ?? ''}/${item.path}`}
              className={classes.link}
            >
              <Card className={classes.card} h="100%">
                <Group justify="space-between" wrap="nowrap" gap="sm">
                  <Text fw={700} size="lg" className={classes.title}>
                    {item.label}
                    <IconChevronRight size={16} />
                  </Text>
                  {badgeOf(item)}
                </Group>
                <Text size="sm" c="dimmed" mt={4}>
                  {item.description}
                </Text>
              </Card>
            </UnstyledButton>
          ) : (
            <Card key={item.key} className={classes.pending} h="100%">
              <Text fw={700} size="lg" c="dimmed">
                {item.label}
              </Text>
              <Text size="sm" c="dimmed" mt={4}>
                {item.description}
              </Text>
              <Badge variant="light" color="gray" mt="sm">
                준비 중 · {item.owner}
              </Badge>
            </Card>
          ),
        )}
      </SimpleGrid>
    </Stack>
  );
}
