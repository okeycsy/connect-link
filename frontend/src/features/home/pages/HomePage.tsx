import {
  Badge,
  Box,
  Card,
  Chip,
  Group,
  SimpleGrid,
  Skeleton,
  Stack,
  Table,
  Text,
  Title,
} from '@mantine/core';
import { useState } from 'react';

import {
  useBusinesses,
  useCurrentBusiness,
  useOrganizations,
  type OrganizationResponse,
} from '../../organization/hooks';
import classes from './HomePage.module.css';

type TypeFilter = 'ALL' | OrganizationResponse['type'];

/**
 * 홈: "지금 할 일" 카드 대시보드의 틀. 남색 밴드 위 글래스 KPI 타일 네 개 + 카드.
 * 각 기능 프로젝트가 자기 카드를 하나씩 끼운다. 지금은 기준정보 카드로 API 연결만 보여 준다.
 */
export function HomePage() {
  const business = useCurrentBusiness();
  const { data: businesses } = useBusinesses();
  const { data: organizations, isPending, isError } = useOrganizations();
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('ALL');

  const financeCount = organizations?.filter((o) => o.type === 'FINANCE_ORGANIZATION').length;
  const filtered =
    organizations?.filter((o) => typeFilter === 'ALL' || o.type === typeFilter) ?? [];

  return (
    <Stack gap="lg">
      <Box className={classes.band}>
        <Text c="white" size="sm" opacity={0.75}>
          {business?.label ?? '업무 영역'}
        </Text>
        <Title order={2} c="white" mb="md">
          안녕하세요, 지금 할 일을 모았습니다.
        </Title>
        <SimpleGrid cols={{ base: 2, md: 4 }} spacing="sm">
          <KpiTile label="사용 중인 기관" value={organizations?.length} unit="곳" />
          <KpiTile label="참가기관" value={financeCount} unit="곳" />
          <KpiTile label="업무 영역" value={businesses?.length} unit="개" />
          <KpiTile label="회신 대기 의견 요청" value={undefined} unit="건" />
        </SimpleGrid>
      </Box>

      <Card data-testid="organization-card">
        <Group justify="space-between" mb="md">
          <div>
            <Text fw={700} size="xl">
              기관 목록
            </Text>
            <Text c="dimmed" size="sm">
              기준정보 · 샘플 데이터
            </Text>
          </div>
          <Chip.Group value={typeFilter} onChange={(v) => setTypeFilter(v as TypeFilter)}>
            <Group gap="xs">
              <Chip value="ALL" size="xs">
                전체
              </Chip>
              <Chip value="KFTC" size="xs">
                결제원
              </Chip>
              <Chip value="FINANCE_ORGANIZATION" size="xs">
                참가기관
              </Chip>
              <Chip value="USE_ORGANIZATION" size="xs">
                이용기관
              </Chip>
            </Group>
          </Chip.Group>
        </Group>

        {isError && (
          <Text c="red" size="sm">
            기관 목록을 불러오지 못했습니다.
          </Text>
        )}
        {isPending ? (
          <Stack gap="xs">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} h={36} />
            ))}
          </Stack>
        ) : (
          <Table verticalSpacing="sm" highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th w={56} />
                <Table.Th>기관</Table.Th>
                <Table.Th>구분</Table.Th>
                <Table.Th>코드</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {filtered.map((o) => (
                <Table.Tr key={o.id}>
                  <Table.Td>
                    <Box className={classes.monogram}>{o.name.slice(0, 2)}</Box>
                  </Table.Td>
                  <Table.Td>
                    <Text fw={600}>{o.name}</Text>
                  </Table.Td>
                  <Table.Td>
                    <Badge variant="light">{o.typeLabel}</Badge>
                  </Table.Td>
                  <Table.Td>
                    <Text ff="monospace" size="sm" c="dimmed">
                      {o.code}
                    </Text>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
      </Card>
    </Stack>
  );
}

function KpiTile({ label, value, unit }: { label: string; value?: number; unit: string }) {
  return (
    <Box className={classes.kpi}>
      <Text size="sm" c="white" opacity={0.75}>
        {label}
      </Text>
      <Group gap={4} align="baseline">
        <Text className={classes.kpiValue} c="white">
          {value ?? '—'}
        </Text>
        <Text size="sm" c="white" opacity={0.75}>
          {unit}
        </Text>
      </Group>
    </Box>
  );
}
