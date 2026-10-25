import { Box, Grid, Group, SimpleGrid, Skeleton, Stack, Text, Title } from '@mantine/core';
import { useParams } from 'react-router-dom';

import { HomeCardView } from '../components/HomeCardView';
import { StateView } from '../../../shared/state/StateView';
import { useHome, type HomeCard } from '../hooks';
import classes from './HomePage.module.css';

/** 윗줄 오른쪽 큰 칸에 두는 자리. SHY 요청(2026-10-24). */
const MAIN_KEY: HomeCard['key'] = 'SUSPENSIONS';
/** 윗줄 왼쪽 칸 후보. 이 사람에게 있는 첫 카드 하나를 둔다. SHY 요청(2026-10-24). */
const SIDE_CANDIDATES: HomeCard['key'][] = ['SIGNUP_APPROVAL', 'OPINION_REPLIES'];

/**
 * 홈: "지금 할 일". 인사말과 날짜 태그를 한 줄에 두고(설명 문구 없음, SHY 요청 2026-10-24), 그 아래 카드.
 * 1280×800 데스크톱에서 스크롤 없이 한 화면에 들어오게 여백·카드 안 간격을 줄였다. SHY 요청(2026-10-25).
 * 건수 요약(KPI 타일)은 두지 않고, 서버가 이 사람에게 할 일이 있는 카드만 준다. SHY 요청(2026-10-24).
 * 결제원 담당자: 윗줄 왼쪽은 가입 승인 대기(없으면 회신 대기 의견 요청), 오른쪽 큰 칸은 다가오는 일시중단이다.
 * 기관 담당자: 요청받은 일(연락처 변경·확인, 의견 회신, 기관 테스트, 우리 기관을 부른 대화) 카드를 한 줄에 두 장씩 놓는다. 임박·초과 글자 배지는 두지 않는다.
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
  // 윗줄은 일시중단 카드가 있을 때(결제원 담당자)만 둔다. 기관 담당자 홈은 요청받은 일 카드만 한 줄에 놓는다.
  const side = main
    ? SIDE_CANDIDATES.map((k) => cards.find((c) => c.key === k)).find(
        (c): c is HomeCard => c !== undefined,
      )
    : undefined;
  const bottom = cards.filter((c) => c !== main && c !== side);

  return (
    <Stack gap="md" className={classes.home}>
      {/* 인사말과 날짜 태그를 한 줄에 둔다. 홈이 스크롤 없이 한 화면에 들어오게. SHY 요청(2026-10-25). */}
      <Group gap="md" align="center" wrap="wrap" className={classes.head}>
        <Title order={1} className={classes.greeting}>
          {data ? `안녕하세요, ${data.userName} 님` : '안녕하세요'}
        </Title>
        <Box className={classes.dateTag}>
          {formatToday()} · {data?.businessLabel ?? '업무 영역'}
        </Box>
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
          {main && (
            <Grid gutter="md">
              {/* 윗줄 두 칸은 높이를 맞춘다(tall). SHY 요청(2026-10-24). */}
              {side && (
                <Grid.Col span={{ base: 12, md: 5 }}>
                  <HomeCardView card={side} tall />
                </Grid.Col>
              )}
              <Grid.Col span={{ base: 12, md: side ? 7 : 12 }}>
                <HomeCardView card={main} tall />
              </Grid.Col>
            </Grid>
          )}
          <SimpleGrid
            // 기관 담당자 홈(윗줄 없음)은 한 줄에 두 장씩 놓고 같은 줄 카드 높이를 맞춘다. SHY 요청(2026-10-24).
            cols={
              main
                ? { base: 1, sm: Math.min(bottom.length, 2), md: Math.min(bottom.length, 3) }
                : { base: 1, sm: 2 }
            }
            spacing="md"
          >
            {bottom.map((c) => (
              <HomeCardView key={c.key} card={c} tall={!main} />
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
