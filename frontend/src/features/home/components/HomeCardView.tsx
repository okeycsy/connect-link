import { Anchor, Badge, Box, Card, Group, Stack, Text, Tooltip } from '@mantine/core';
import { Link } from 'react-router-dom';

import { formatDate } from '../../../shared/format';
import type { HomeCard, HomeItem } from '../hooks';
import classes from './HomeCardView.module.css';
import { SuspensionUpcoming } from './SuspensionUpcoming';

/** 일시중단 밖 카드에 보이는 할 일 줄 수. 나머지는 "모두 보기" 에서 본다. SHY 요청(2026-10-24). */
const MAX_ROWS = 3;

/** 기한 배지 색: 초과 빨강 채움, 임박 앰버, 여유 스카이. frontend/CLAUDE.md "상태 색". */
const URGENCY_COLOR = { OVERDUE: 'red', SOON: 'amber', NONE: 'sky' } as const;

interface Props {
  card: HomeCard;
  /** 세로로 쌓이는 자리(오른쪽 열). 높이를 채우지 않는다. */
  stacked?: boolean;
  /** 왼쪽 큰 칸. 남는 높이를 내용(다가오는 일시중단 목록)이 채운다. */
  tall?: boolean;
}

/**
 * 홈 "지금 할 일" 카드. 제목, 짧은 요약 한 줄, 할 일 줄만 그린다. 숫자 줄과 진행 막대는 홈에 두지 않고 "모두 보기" 화면에서 본다
 * (SHY 요청, 2026-10-24). 무엇을 채울지는 기능 모듈이 정한다.
 */
export function HomeCardView({ card, stacked = false, tall = false }: Props) {
  const header = (
    <Group gap="xs" wrap="wrap" style={{ flexShrink: 0 }}>
      <Text fw={700} size="xl" style={{ whiteSpace: 'nowrap' }}>
        {card.title}
      </Text>
      {card.source === 'SAMPLE' && (
        <Tooltip label="기능 프로젝트가 붙기 전까지 공통이 넣어 둔 샘플입니다." withArrow>
          <Badge size="sm" variant="outline" color="gray" style={{ flexShrink: 0 }}>
            샘플
          </Badge>
        </Tooltip>
      )}
    </Group>
  );

  const more = card.link && (
    <Anchor component={Link} to={card.link} size="sm" style={{ whiteSpace: 'nowrap' }}>
      모두 보기
    </Anchor>
  );

  const suspensions = card.key === 'SUSPENSIONS';

  return (
    <Card
      className={classes.card}
      data-stacked={stacked || undefined}
      data-tall={tall || undefined}
      data-urgency={card.urgency !== 'NONE' ? card.urgency : undefined}
      data-testid={`home-card-${card.key}`}
    >
      <Group justify="space-between" align="flex-start" mb="xs" wrap="nowrap">
        {header}
        {more}
      </Group>

      <Stack gap="sm" className={classes.body}>
        {card.summary && (
          <Text size="sm" c="dimmed">
            {card.summary}
          </Text>
        )}

        {suspensions ? (
          <SuspensionUpcoming items={card.items} fill={tall} />
        ) : (
          card.items.length > 0 && (
            <Stack gap={8} className={classes.items}>
              {card.items.slice(0, MAX_ROWS).map((item, i) => (
                <ItemRow key={i} item={item} />
              ))}
            </Stack>
          )
        )}
      </Stack>
    </Card>
  );
}

function ItemRow({ item }: { item: HomeItem }) {
  const content = (
    <Group justify="space-between" wrap="nowrap" className={classes.item}>
      <Group gap="sm" wrap="nowrap" style={{ minWidth: 0 }}>
        <Box
          className={classes.dot}
          data-urgency={item.urgency !== 'NONE' ? item.urgency : undefined}
        />
        <div style={{ minWidth: 0 }}>
          <Text size="sm" fw={600} truncate>
            {item.title}
          </Text>
          {item.subtitle && (
            <Text size="xs" c="dimmed" truncate>
              {item.subtitle}
            </Text>
          )}
        </div>
      </Group>
      {item.dueAt && (
        <Badge
          size="sm"
          variant={item.urgency === 'OVERDUE' ? 'filled' : 'light'}
          color={URGENCY_COLOR[item.urgency]}
          className={classes.number}
          style={{ flexShrink: 0 }}
        >
          {formatDate(item.dueAt)}
        </Badge>
      )}
    </Group>
  );
  return item.link ? (
    <Anchor component={Link} to={item.link} underline="never" c="inherit">
      {content}
    </Anchor>
  ) : (
    content
  );
}
