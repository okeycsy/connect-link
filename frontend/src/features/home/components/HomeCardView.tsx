import {
  Anchor,
  Badge,
  Box,
  Card,
  Group,
  Progress,
  RingProgress,
  Stack,
  Text,
  Tooltip,
} from '@mantine/core';
import { Link } from 'react-router-dom';

import { formatDate } from '../../../shared/format';
import type { HomeCard, HomeItem } from '../hooks';
import classes from './HomeCardView.module.css';
import { SuspensionUpcoming } from './SuspensionUpcoming';

/** 기한 배지 색: 초과 빨강 채움, 임박 앰버, 여유 스카이. frontend/CLAUDE.md "상태 색". */
const URGENCY_COLOR = { OVERDUE: 'red', SOON: 'amber', NONE: 'sky' } as const;

interface Props {
  card: HomeCard;
  /** 중요도가 낮은 자리는 한 줄짜리 띠로 작게 보인다(제목, 요약, 숫자만). */
  compact?: boolean;
  /** 세로로 쌓이는 자리(오른쪽 열). 높이를 채우지 않는다. */
  stacked?: boolean;
  /** 왼쪽 큰 칸. 남는 높이를 내용(다가오는 일시중단 목록)이 채운다. */
  tall?: boolean;
}

/** 홈 "지금 할 일" 카드. 서버가 준 모양(요약, 숫자, 진행률, 항목)만 그리고 무엇을 채울지는 기능 모듈이 정한다. */
export function HomeCardView({ card, compact = false, stacked = false, tall = false }: Props) {
  const header = (
    <Group gap="xs" wrap={compact ? 'nowrap' : 'wrap'} style={{ flexShrink: 0 }}>
      <Text fw={700} size={compact ? 'lg' : 'xl'} style={{ whiteSpace: 'nowrap' }}>
        {card.title}
      </Text>
      {card.source === 'SAMPLE' && (
        <Tooltip label="기능 프로젝트가 붙기 전까지 공통이 넣어 둔 샘플입니다." withArrow>
          <Badge size="sm" variant="outline" color="gray" style={{ flexShrink: 0 }}>
            샘플
          </Badge>
        </Tooltip>
      )}
      {card.urgency === 'OVERDUE' && (
        <Badge size="sm" color="red" variant="filled" style={{ flexShrink: 0 }}>
          초과
        </Badge>
      )}
      {card.urgency === 'SOON' && (
        <Badge size="sm" color="amber" style={{ flexShrink: 0 }}>
          임박
        </Badge>
      )}
    </Group>
  );

  const more = card.link && (
    <Anchor component={Link} to={card.link} size="sm" style={{ whiteSpace: 'nowrap' }}>
      모두 보기
    </Anchor>
  );

  if (compact) {
    return (
      <Card
        className={classes.card}
        data-compact
        data-urgency={card.urgency !== 'NONE' ? card.urgency : undefined}
        data-testid={`home-card-${card.key}`}
      >
        <Group justify="space-between" gap="sm">
          <Group gap="lg" wrap="nowrap" style={{ minWidth: 0 }}>
            {header}
            <Text size="sm" c="dimmed" truncate visibleFrom="md">
              {card.summary}
            </Text>
          </Group>
          <Group gap="lg" wrap="nowrap">
            {card.metrics.map((m) => (
              <Text key={m.label} size="sm" c="dimmed" style={{ whiteSpace: 'nowrap' }}>
                {m.label}{' '}
                <Text span fw={700} size="md" c="var(--cl-ink)" className={classes.number}>
                  {m.value}
                </Text>
                {m.unit}
              </Text>
            ))}
            {more}
          </Group>
        </Group>
      </Card>
    );
  }

  const ring = card.key === 'UPDATE_CAMPAIGN' && card.progress;
  const suspensions = card.key === 'SUSPENSIONS';
  const percent = card.progress ? (card.progress.done / Math.max(card.progress.total, 1)) * 100 : 0;

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

        {!suspensions && (
          <Group gap="lg" align="center" wrap="nowrap">
            {ring && (
              <RingProgress
                size={76}
                thickness={8}
                roundCaps
                sections={[{ value: percent, color: 'clay' }]}
                label={
                  <Text ta="center" fw={700} size="md" className={classes.number}>
                    {Math.round(percent)}%
                  </Text>
                }
              />
            )}
            <Group gap="xl">
              {card.metrics.map((m) => (
                <div key={m.label}>
                  <Text size="xs" c="dimmed">
                    {m.label}
                  </Text>
                  <Group gap={2} align="baseline">
                    <Text className={classes.number} fw={700} size="xl">
                      {m.value}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {m.unit}
                    </Text>
                  </Group>
                </div>
              ))}
            </Group>
          </Group>
        )}

        {card.progress && !ring && (
          <div>
            <Group justify="space-between" mb={4}>
              <Text size="xs" c="dimmed">
                {card.progress.label}
              </Text>
              <Text size="xs" c="dimmed" className={classes.number}>
                {card.progress.done}/{card.progress.total}
              </Text>
            </Group>
            <Progress value={percent} size="sm" radius="xl" color="clay" />
          </div>
        )}

        {suspensions ? (
          <SuspensionUpcoming items={card.items} fill={tall} />
        ) : (
          card.items.length > 0 && (
            <Stack gap={8} className={classes.items}>
              {card.items.map((item, i) => (
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
