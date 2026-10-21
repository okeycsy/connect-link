import { Box, SimpleGrid, Text, Tooltip, UnstyledButton } from '@mantine/core';
import dayjs from 'dayjs';
import { Link } from 'react-router-dom';

import type { HomeItem } from '../hooks';
import classes from './SuspensionWeek.module.css';

const WEEKDAYS = ['월', '화', '수', '목', '금'];

interface Props {
  items: HomeItem[];
  /** 기준일. 테스트에서만 넘긴다. */
  today?: dayjs.Dayjs;
  /** 큰 칸에 놓여 남는 높이를 요일 칸이 채운다. */
  fill?: boolean;
}

/** 서버(Asia/Seoul)와 같은 날짜. 브라우저 시간대가 달라도 서버가 정한 "오늘" 과 맞춘다. */
function seoulToday() {
  return dayjs(new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Seoul' }));
}

/**
 * 이번 주 일시중단: 월~금 다섯 칸에 일시중단 건을 날짜(dueAt)별로 놓는다. 오늘 칸은 clay-1 바탕.
 * 오늘 건(임박)은 clay, 나머지는 sky 칩이다. 주말이면 그 주의 월~금을 보인다.
 */
export function SuspensionWeek({ items, today = seoulToday(), fill = false }: Props) {
  const offset = (today.day() + 6) % 7; // 월요일 0
  const monday = today.subtract(offset, 'day').startOf('day');
  const days = WEEKDAYS.map((label, i) => {
    const date = monday.add(i, 'day');
    const key = date.format('YYYY-MM-DD');
    return {
      key,
      label: `${label} ${date.date()}`,
      today: date.isSame(today, 'day'),
      items: items.filter((it) => it.dueAt === key),
    };
  });

  return (
    <SimpleGrid
      cols={5}
      spacing={8}
      className={classes.week}
      data-fill={fill || undefined}
      data-testid="suspension-week"
    >
      {days.map((d) => (
        <Box key={d.key} className={classes.day} data-today={d.today || undefined}>
          <Text size="sm" fw={600} ta="center" c="var(--cl-ink-soft)">
            {d.label}
          </Text>
          {d.items.map((it, i) => (
            <Tooltip key={i} label={it.subtitle ?? it.title} withArrow disabled={!it.subtitle}>
              <UnstyledButton
                component={Link}
                to={it.link ?? '#'}
                className={classes.chip}
                data-soon={it.urgency !== 'NONE' || undefined}
              >
                {it.title}
              </UnstyledButton>
            </Tooltip>
          ))}
        </Box>
      ))}
    </SimpleGrid>
  );
}
