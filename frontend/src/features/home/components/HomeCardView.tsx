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

/** 기한 배지 색: 초과 빨강 채움, 임박 앰버, 여유 스카이. frontend/CLAUDE.md "상태 색". */
const URGENCY_COLOR = { OVERDUE: 'red', SOON: 'amber', NONE: 'sky' } as const;

interface Props {
  card: HomeCard;
  /** 가장 급한 카드 하나는 크게 보인다. */
  featured?: boolean;
}

/** 홈 "지금 할 일" 카드. 서버가 준 모양(요약, 숫자, 진행률, 항목)만 그리고 무엇을 채울지는 기능 모듈이 정한다. */
export function HomeCardView({ card, featured = false }: Props) {
  const unavailable = card.source === 'UNAVAILABLE';
  const ring = card.key === 'UPDATE_CAMPAIGN' && card.progress;

  return (
    <Card
      className={classes.card}
      data-featured={featured || undefined}
      data-urgency={card.urgency !== 'NONE' ? card.urgency : undefined}
      data-testid={`home-card-${card.key}`}
    >
      <Group justify="space-between" align="flex-start" mb="xs" wrap="nowrap">
        <Group gap="xs">
          <Text fw={700} size={featured ? 'xl' : 'lg'}>
            {card.title}
          </Text>
          {card.source === 'SAMPLE' && (
            <Tooltip label="기능 프로젝트가 붙기 전까지 공통이 넣어 둔 샘플입니다." withArrow>
              <Badge size="sm" variant="outline" color="gray">
                샘플
              </Badge>
            </Tooltip>
          )}
          {card.urgency === 'OVERDUE' && (
            <Badge size="sm" color="red" variant="filled">
              초과
            </Badge>
          )}
          {card.urgency === 'SOON' && (
            <Badge size="sm" color="amber">
              임박
            </Badge>
          )}
        </Group>
        {card.link && !unavailable && (
          <Anchor component={Link} to={card.link} size="sm" style={{ whiteSpace: 'nowrap' }}>
            모두 보기
          </Anchor>
        )}
      </Group>

      {unavailable ? (
        <Text c="dimmed" size="sm">
          해당 기능 프로젝트가 이 카드를 연결하면 여기에 보입니다.
        </Text>
      ) : (
        <Stack gap="sm">
          {card.summary && (
            <Text size="sm" c="dimmed">
              {card.summary}
            </Text>
          )}

          <Group gap="lg" align="center" wrap="nowrap">
            {ring && (
              <RingProgress
                size={featured ? 96 : 76}
                thickness={8}
                roundCaps
                sections={[
                  {
                    value: (card.progress!.done / Math.max(card.progress!.total, 1)) * 100,
                    color: 'clay',
                  },
                ]}
                label={
                  <Text
                    ta="center"
                    fw={700}
                    size={featured ? 'lg' : 'md'}
                    className={classes.number}
                  >
                    {Math.round((card.progress!.done / Math.max(card.progress!.total, 1)) * 100)}%
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
                    <Text className={classes.number} fw={700} size={featured ? '28px' : 'xl'}>
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
              <Progress
                value={(card.progress.done / Math.max(card.progress.total, 1)) * 100}
                size="sm"
                radius="xl"
                color="clay"
              />
            </div>
          )}

          {card.items.length > 0 && (
            <Stack gap={8} className={classes.items}>
              {card.items.map((item, i) => (
                <ItemRow key={i} item={item} />
              ))}
            </Stack>
          )}
        </Stack>
      )}
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
