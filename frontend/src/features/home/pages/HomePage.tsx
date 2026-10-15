import { Box, Grid, Group, SimpleGrid, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useParams } from 'react-router-dom';

import { HomeCardView } from '../components/HomeCardView';
import { useHome, type HomeCard, type HomeKpi } from '../hooks';
import classes from './HomePage.module.css';

const URGENCY_RANK = { OVERDUE: 2, SOON: 1, NONE: 0 } as const;

/**
 * 홈: "지금 할 일". 남색 밴드 위 글래스 KPI 타일 네 개, 가장 급한 카드 하나를 크게, 나머지 카드를 작게.
 * 카드 내용은 서버(/api/v1/home)가 자리별 기여자에게서 모아 준다. 업무 영역(레일)마다 다르다.
 */
export function HomePage() {
  const { business } = useParams<{ business: string }>();
  const { data, isPending, isError } = useHome(business);

  const featured = data ? pickFeatured(data.cards) : undefined;
  const rest = data?.cards.filter((c) => c !== featured) ?? [];

  return (
    <Stack gap="lg">
      <Box className={classes.band}>
        <Text c="white" size="sm" opacity={0.75}>
          {data?.businessLabel ?? '업무 영역'}
        </Text>
        <Title order={2} c="white" mb="md">
          {data
            ? `안녕하세요, ${data.userName} 님. 지금 할 일을 모았습니다.`
            : '지금 할 일을 모으는 중입니다.'}
        </Title>
        <SimpleGrid cols={{ base: 2, md: 4 }} spacing="sm">
          {data
            ? data.kpis.map((k) => <KpiTile key={k.label} kpi={k} />)
            : [0, 1, 2, 3].map((i) => <Skeleton key={i} h={72} radius="md" opacity={0.4} />)}
        </SimpleGrid>
      </Box>

      {isError && (
        <Text c="red" size="sm">
          홈 정보를 불러오지 못했습니다.
        </Text>
      )}

      {isPending || !data ? (
        <SimpleGrid cols={{ base: 1, md: 3 }} spacing="md">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} h={180} radius="md" />
          ))}
        </SimpleGrid>
      ) : (
        <Stack gap="md" data-testid="home-cards">
          <Grid gutter="md">
            {featured && (
              <Grid.Col span={{ base: 12, md: 7 }}>
                <HomeCardView card={featured} featured />
              </Grid.Col>
            )}
            <Grid.Col span={{ base: 12, md: featured ? 5 : 12 }}>
              <Stack gap="md" h="100%">
                {rest.slice(0, 2).map((c) => (
                  <HomeCardView key={c.key} card={c} />
                ))}
              </Stack>
            </Grid.Col>
          </Grid>
          <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
            {rest.slice(2).map((c) => (
              <HomeCardView key={c.key} card={c} />
            ))}
          </SimpleGrid>
        </Stack>
      )}
    </Stack>
  );
}

/** 가장 급한 카드. 급한 정도가 같으면 자리 순서가 앞선 것. */
function pickFeatured(cards: HomeCard[]): HomeCard | undefined {
  return cards.reduce<HomeCard | undefined>((best, c) => {
    if (c.source === 'UNAVAILABLE') return best;
    if (!best || URGENCY_RANK[c.urgency] > URGENCY_RANK[best.urgency]) return c;
    return best;
  }, undefined);
}

function KpiTile({ kpi }: { kpi: HomeKpi }) {
  return (
    <Box className={classes.kpi} data-urgent={kpi.urgent || undefined}>
      <Group gap={6} wrap="nowrap">
        {kpi.urgent && <Box className={classes.kpiDot} aria-label="급함" />}
        <Text size="sm" c="white" opacity={0.75} truncate>
          {kpi.label}
        </Text>
      </Group>
      <Group gap={4} align="baseline">
        <Text className={classes.kpiValue} c="white">
          {kpi.value}
        </Text>
        <Text size="sm" c="white" opacity={0.75}>
          {kpi.unit}
        </Text>
      </Group>
    </Box>
  );
}
