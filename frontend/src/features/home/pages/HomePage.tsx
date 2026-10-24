import { Box, Grid, Group, SimpleGrid, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useParams } from 'react-router-dom';

import { HomeCardView } from '../components/HomeCardView';
import { StateView } from '../../../shared/state/StateView';
import { useHome, type HomeCard } from '../hooks';
import classes from './HomePage.module.css';

/** 왼쪽 큰 칸에 두는 자리. 일시중단을 먼저 읽히게 한다. SHY 요청(2026-10-21). */
const MAIN_KEY: HomeCard['key'] = 'SUSPENSIONS';
/** 오른쪽 열에 두는 자리. 나머지는 아래 줄에 서버 순서대로 놓는다. */
const SIDE_KEYS: HomeCard['key'][] = ['OPINION_REPLIES'];

/**
 * 홈: "지금 할 일". 파스텔 번짐 위에 날짜 태그와 인사말만 두고(설명 문구 없음, SHY 요청 2026-10-24), 그 아래 카드.
 * 건수 요약(KPI 타일)은 두지 않고, 서버가 이 사람에게 할 일이 있는 카드만 준다. SHY 요청(2026-10-24).
 * 왼쪽 큰 칸은 다가오는 일시중단, 오른쪽은 회신 대기 의견 요청이다. 둘 중 하나만 있으면 한 줄을 다 쓴다.
 * 아래 줄은 나머지 카드이고, 장 수만큼 열을 나눠 한 줄을 채운다(최대 3열). 6개월 지난 담당자 확인은 홈 카드 대신
 * 로그인 뒤 확인 창이 맡는다. SHY 요청(2026-10-24).
 * 최근 알림은 홈에 두지 않고 상단 바의 알림 벨에서 본다. SHY 요청(2026-10-21).
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
  const bottom = cards.filter((c) => c.key !== MAIN_KEY && !SIDE_KEYS.includes(c.key));

  return (
    <Stack gap="lg" className={classes.home}>
      <Group justify="space-between" align="flex-end" gap="md">
        <Stack gap={10} align="flex-start">
          <Box className={classes.dateTag}>
            {formatToday()} · {data?.businessLabel ?? '업무 영역'}
          </Box>
          <Title order={1} className={classes.greeting}>
            {data ? `안녕하세요, ${data.userName} 님` : '안녕하세요'}
          </Title>
        </Stack>
      </Group>

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
      ) : cards.length === 0 ? (
        <Box className={classes.empty}>
          <StateView
            kind="empty"
            title="지금 할 일이 없습니다"
            description="담당하는 연락처, 의견 요청, 기관 테스트, 일시중단에 할 일이 생기면 여기에 모입니다."
          />
        </Box>
      ) : (
        <Stack gap="md" data-testid="home-cards">
          {(main || side.length > 0) && (
            <Grid gutter="md">
              {main && (
                <Grid.Col span={{ base: 12, md: side.length > 0 ? 7 : 12 }}>
                  <HomeCardView card={main} tall />
                </Grid.Col>
              )}
              {side.length > 0 && (
                <Grid.Col span={{ base: 12, md: main ? 5 : 12 }}>
                  <Stack gap="md" h="100%">
                    {/* 오른쪽 카드가 하나면 왼쪽 큰 칸과 높이를 맞춘다. SHY 요청(2026-10-24). */}
                    {side.map((c) =>
                      main && side.length === 1 ? (
                        <HomeCardView key={c.key} card={c} tall />
                      ) : (
                        <HomeCardView key={c.key} card={c} stacked />
                      ),
                    )}
                  </Stack>
                </Grid.Col>
              )}
            </Grid>
          )}
          <SimpleGrid
            cols={{ base: 1, sm: Math.min(bottom.length, 2), md: Math.min(bottom.length, 3) }}
            spacing="md"
          >
            {bottom.map((c) => (
              <HomeCardView key={c.key} card={c} />
            ))}
          </SimpleGrid>
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
