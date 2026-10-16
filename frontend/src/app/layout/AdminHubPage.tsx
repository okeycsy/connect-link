import { Badge, Card, SimpleGrid, Stack, Text, UnstyledButton } from '@mantine/core';
import { IconChevronRight } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

import { useCurrentBusiness } from '../../features/organization/hooks';
import { ADMIN_MENU } from '../navigation';
import classes from './AdminHubPage.module.css';
import { PageHeader } from '../../shared/PageHeader';

/** 관리 그룹의 첫 화면. 항목 카드를 늘어놓고, 아직 화면이 없는 자리는 준비 중으로 표시한다. */
export function AdminHubPage() {
  const business = useCurrentBusiness();
  return (
    <Stack gap="md">
      <PageHeader
        title="관리"
        description={
          <>{business?.label ?? '업무 영역'}의 기준정보와 운영 설정. 결제원 권한자만 봅니다.</>
        }
      />
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md" data-testid="admin-menu">
        {ADMIN_MENU.map((item) =>
          item.path ? (
            <UnstyledButton
              key={item.key}
              component={Link}
              to={`/${business?.slug ?? ''}/${item.path}`}
              className={classes.link}
            >
              <Card className={classes.card} h="100%">
                <Text fw={700} size="lg" className={classes.title}>
                  {item.label}
                  <IconChevronRight size={16} />
                </Text>
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
