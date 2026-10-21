import { Box, Grid, Group, SimpleGrid, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useParams } from 'react-router-dom';

import { HomeCardView } from '../components/HomeCardView';
import { RecentNotificationsCard } from '../components/RecentNotificationsCard';
import { useHome, type HomeCard, type HomeKpi } from '../hooks';
import classes from './HomePage.module.css';

/** 왼쪽 큰 칸에 두는 자리. 일시중단을 먼저 읽히게 한다. SHY 요청(2026-10-21). */
const MAIN_KEY: HomeCard['key'] = 'SUSPENSIONS';
/** 오른쪽 열에 두는 자리. 나머지는 아래 줄에 서버 순서대로 놓는다. */
const SIDE_KEYS: HomeCard['key'][] = ['OPINION_REPLIES'];
/** 중요도가 낮아 아래 줄 끝에 작게 두는 자리. SHY 요청(2026-10-15). */
const COMPACT_KEYS: HomeCard['key'][] = ['CONTACT_CONFIRM'];

/**
 * 홈: "지금 할 일". 파스텔 번짐 위에 인사말과 흰 KPI 타일 네 개.
 * 그 아래 왼쪽 큰 칸은 이번 주 일시중단, 오른쪽은 회신 대기 의견 요청이다. 두 칸은 높이를 맞춰 한 화면에 들어온다.
 * 아래 줄은 나머지 카드이고 확인이 필요한 담당자는 끝에 작게, 최근 소식(알림 센터와 같은 내용)은 맨 아래에 둔다.
 * 카드 내용은 서버(/api/v1/home)가 자리별 기여자에게서 모아 준다. 업무 영역(레일)마다 다르다.
 */
export function HomePage() {
  const { business } = useParams<{ business: string }>();
  const { data, isPending, isError } = useHome(business);

  const cards = data?.cards ?? [];
  const main = cards.find((c) => c.key === MAIN_KEY);
  const side = SIDE_KEYS.map((k) => cards.find((c) => c.key === k)).filter(
    (c): c is HomeCard => c !== undefined,
  );
  const bottom = cards.filter(
    (c) => c.key !== MAIN_KEY && !SIDE_KEYS.includes(c.key) && !COMPACT_KEYS.includes(c.key),
  );
  const compact = cards.filter((c) => COMPACT_KEYS.includes(c.key));

  return (
    <Stack gap="lg" className={classes.home}>
      <Group justify="space-between" align="flex-end" gap="md">
        <Stack gap={10} align="flex-start">
          <Box className={classes.dateTag}>
            {formatToday()} · {data?.businessLabel ?? '업무 영역'}
          </Box>
          <Title order={1} className={classes.greeting}>
            {data ? `안녕하세요, ${data.userName} 님` : '지금 할 일을 모으는 중입니다.'}
          </Title>
          {data && (
            <Text size="lg" c="var(--cl-ink-soft)">
              지금 할 일을 모았습니다.
            </Text>
          )}
        </Stack>
      </Group>

      <SimpleGrid cols={{ base: 2, md: 4 }} spacing={14}>
        {data
          ? data.kpis.map((k) => <KpiTile key={k.label} kpi={k} />)
          : [0, 1, 2, 3].map((i) => <Skeleton key={i} h={112} radius={22} />)}
      </SimpleGrid>

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
            {main && (
              <Grid.Col span={{ base: 12, md: 7 }}>
                <HomeCardView card={main} tall />
              </Grid.Col>
            )}
            <Grid.Col span={{ base: 12, md: 5 }}>
              <Stack gap="md" h="100%">
                {side.map((c) => (
                  <HomeCardView key={c.key} card={c} stacked />
                ))}
              </Stack>
            </Grid.Col>
          </Grid>
          <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
            {bottom.map((c) => (
              <HomeCardView key={c.key} card={c} />
            ))}
          </SimpleGrid>
          {compact.map((c) => (
            <HomeCardView key={c.key} card={c} compact />
          ))}
          <RecentNotificationsCard />
        </Stack>
      )}
    </Stack>
  );
}

/** 머리 날짜 태그. 예: "10월 15일 목요일". */
function formatToday() {
  return new Date().toLocaleDateString('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'long',
    timeZone: 'Asia/Seoul',
  });
}

/** KPI 타일: 흰 면 radius 22, 이름(보조 글자) + 큰 숫자(40px) + 단위. 급하면 이름 앞에 빨간 점 하나. */
function KpiTile({ kpi }: { kpi: HomeKpi }) {
  return (
    <Box className={classes.kpi} data-urgent={kpi.urgent || undefined}>
      <Group gap={6} wrap="nowrap">
        {kpi.urgent && <Box className={classes.kpiDot} aria-label="급함" />}
        <Text size="md" c="dimmed" truncate>
          {kpi.label}
        </Text>
      </Group>
      <Group gap={4} align="baseline">
        <Text className={classes.kpiValue}>{kpi.value}</Text>
        <Text size="xl" fw={500}>
          {kpi.unit}
        </Text>
      </Group>
    </Box>
  );
}
