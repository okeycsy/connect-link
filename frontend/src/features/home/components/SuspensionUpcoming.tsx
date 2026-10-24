import { Badge, Box, Group, Stack, Text, UnstyledButton } from '@mantine/core';
import dayjs from 'dayjs';
import { Link } from 'react-router-dom';

import { OrgLogo } from '../../../shared/OrgLogo';
import type { HomeItem } from '../hooks';
import classes from './SuspensionUpcoming.module.css';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

interface Props {
  items: HomeItem[];
  /** 기준일. 테스트에서만 넘긴다. */
  today?: dayjs.Dayjs;
  /** 큰 칸에 놓여 남는 높이를 목록이 채운다. */
  fill?: boolean;
}

interface DayGroup {
  key: string;
  label: string;
  weekday: string;
  weekend: boolean;
  today: boolean;
  items: HomeItem[];
}

/** 서버(Asia/Seoul)와 같은 날짜. 브라우저 시간대가 달라도 서버가 정한 "오늘" 과 맞춘다. */
function seoulToday() {
  return dayjs(new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Seoul' }));
}

/**
 * 다가오는 일시중단: 서버가 준 건(진행 중 + 오늘부터 2주 안에 시작)을 날짜별로 묶어 위에서부터 놓는다.
 * 지난 날은 칸을 두지 않고, 이미 시작한 건은 맨 위 "진행 중" 묶음이다. 일시중단이 몰리는 토·일은 날짜 옆에 주말 배지를 단다.
 * 건이 없는 날은 그리지 않는다. 묶음은 카드 항목의 `dueAt`(시작일)으로만 정한다. SHY 요청(2026-10-24).
 */
export function SuspensionUpcoming({ items, today = seoulToday(), fill = false }: Props) {
  const todayKey = today.format('YYYY-MM-DD');
  const groups: DayGroup[] = [];
  for (const it of items) {
    const started = it.dueAt != null && it.dueAt < todayKey;
    const key = started ? 'ongoing' : (it.dueAt ?? todayKey);
    let group = groups.find((g) => g.key === key);
    if (!group) {
      group = started ? ongoingGroup() : dateGroup(dayjs(key), today);
      groups.push(group);
    }
    group.items.push(it);
  }
  groups.sort((a, b) =>
    a.key === 'ongoing' ? -1 : b.key === 'ongoing' ? 1 : a.key < b.key ? -1 : 1,
  );

  return (
    <Stack
      gap="md"
      className={classes.list}
      data-fill={fill || undefined}
      data-testid="suspension-upcoming"
    >
      {groups.map((g) => (
        <Box key={g.key} data-testid={`suspension-day-${g.key}`}>
          <Group gap={8} mb={6}>
            <Text size="sm" fw={700} c={g.today ? 'clay.7' : 'var(--cl-ink)'}>
              {g.label}
            </Text>
            {g.weekday && (
              <Text size="sm" c="dimmed">
                {g.weekday}
              </Text>
            )}
            {g.weekend && (
              <Badge size="xs" color="sky" variant="light">
                주말
              </Badge>
            )}
          </Group>
          <Stack gap={6}>
            {g.items.map((it, i) => (
              <UnstyledButton
                key={i}
                component={Link}
                to={it.link ?? '#'}
                className={classes.row}
                data-soon={it.urgency !== 'NONE' || undefined}
              >
                <Group gap={6} wrap="nowrap">
                  <OrgLogo code={it.organizationCode} size={18} />
                  <Text size="sm" fw={600} truncate>
                    {it.title}
                  </Text>
                </Group>
                {it.subtitle && (
                  <Text size="xs" c="dimmed" truncate>
                    {it.subtitle}
                  </Text>
                )}
              </UnstyledButton>
            ))}
          </Stack>
        </Box>
      ))}
    </Stack>
  );
}

function ongoingGroup(): DayGroup {
  return {
    key: 'ongoing',
    label: '지금 진행 중',
    weekday: '',
    weekend: false,
    today: true,
    items: [],
  };
}

/** "오늘 10월 24일 (토)", "내일 …", 그 밖에는 "10월 27일 (화)". */
function dateGroup(date: dayjs.Dayjs, today: dayjs.Dayjs): DayGroup {
  const diff = date.diff(today.startOf('day'), 'day');
  const prefix = diff === 0 ? '오늘 · ' : diff === 1 ? '내일 · ' : '';
  return {
    key: date.format('YYYY-MM-DD'),
    label: `${prefix}${date.month() + 1}월 ${date.date()}일`,
    weekday: `(${WEEKDAYS[date.day()]})`,
    weekend: date.day() === 0 || date.day() === 6,
    today: diff === 0,
    items: [],
  };
}
