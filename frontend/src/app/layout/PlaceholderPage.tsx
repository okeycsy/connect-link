import { Badge, Card, Stack, Text } from '@mantine/core';

import { useCurrentBusiness } from '../../features/organization/hooks';
import { PageHeader } from '../../shared/PageHeader';

interface Props {
  title: string;
  description: string;
}

/** 아직 만들지 않은 기능 화면의 자리. 각 기능 프로젝트가 이 자리를 실제 화면으로 바꾼다. */
export function PlaceholderPage({ title, description }: Props) {
  const business = useCurrentBusiness();
  return (
    <Stack gap="md">
      <PageHeader title={title} description={description} />
      <Card>
        <Stack gap="xs" align="flex-start">
          <Badge variant="light">준비 중</Badge>
          <Text>
            {business?.label ?? '업무 영역'}의 {title} 화면은 해당 기능 프로젝트에서 만듭니다.
          </Text>
        </Stack>
      </Card>
    </Stack>
  );
}
