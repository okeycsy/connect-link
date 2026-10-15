import { Card, SimpleGrid, Stack, Text, Title } from '@mantine/core';

import { StateView, type StateKind } from '../../shared/state/StateView';

const KINDS: { kind: StateKind; when: string }[] = [
  { kind: 'empty', when: '목록·검색 결과가 비었을 때' },
  { kind: 'error', when: '서버 응답이 실패했을 때' },
  { kind: 'forbidden', when: '조회 범위 밖 데이터를 열었을 때(403)' },
  { kind: 'notFound', when: '주소나 항목이 없을 때(404)' },
  { kind: 'internalOnly', when: '내부망 전용 기능을 인터넷망에서 열었을 때(403 INTERNAL_ONLY)' },
];

/** 데모 도구: 빈 화면·오류·권한 없음·주소 없음·내부망 전용 안내를 한곳에서 본다. 실제 화면은 상황이 생길 때 이 모양으로 보인다. */
export function DemoStatesPage() {
  return (
    <Stack gap="md">
      <div>
        <Title order={2}>상태 화면</Title>
        <Text c="dimmed" size="lg">
          기능 화면이 쓰는 공통 안내 다섯 가지. 실제로는 해당 상황이 생길 때만 보입니다.
        </Text>
      </div>
      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md" data-testid="demo-states">
        {KINDS.map((k) => (
          <Card key={k.kind} padding="sm">
            <Text size="xs" c="dimmed" fw={600} px="xs">
              {k.when}
            </Text>
            <StateView kind={k.kind} compact />
          </Card>
        ))}
      </SimpleGrid>
    </Stack>
  );
}
