import { Badge, Card, Group, Stack, Text } from '@mantine/core';

import type { OpinionCollation } from '../hooks';
import classes from './OpinionTable.module.css';

/**
 * 질문별 답 집계(결제원 담당자). 제출한 기관의 답만 센다. 섹션 이동으로 질문을 건너뛴 기관은 따로 센다. 객관식·체크박스는 선택지마다 고른 기관 수와 이름, 주관식은 기관별 답을 보인다.
 * 막대 폭은 제출 기관 수 대비다.
 */
export function QuestionSummary({ collation }: { collation: OpinionCollation }) {
  if (collation.questions.length === 0) return null;
  const replied = Math.max(collation.summary.repliedCount, 1);
  return (
    <Card data-testid="question-summary">
      <Stack gap="md">
        <Text fw={700} size="xl">
          질문별 답
        </Text>
        {collation.questions.map((q, index) => (
          <Stack key={q.questionId} gap={6}>
            <Group gap="xs">
              <Text fw={600}>
                {index + 1}. {q.title}
              </Text>
              <Badge size="sm" color="gray" variant="outline">
                {q.typeLabel}
              </Badge>
              {q.required && (
                <Badge size="sm" color="amber">
                  필수
                </Badge>
              )}
              <Text size="sm" c="dimmed">
                답한 기관 {q.answeredCount}곳
                {q.skippedCount > 0 && ` · 고른 답에 따라 건너뛴 기관 ${q.skippedCount}곳`}
              </Text>
            </Group>
            {q.type === 'TEXT' ? (
              q.textAnswers.length === 0 ? (
                <Text size="sm" c="dimmed">
                  아직 답이 없습니다.
                </Text>
              ) : (
                <Stack gap={4}>
                  {q.textAnswers.map((a) => (
                    <Group key={a.organizationName} gap="sm" wrap="nowrap" align="flex-start">
                      <Text size="sm" fw={600} w={120} style={{ flexShrink: 0 }}>
                        {a.organizationName}
                      </Text>
                      <Text size="sm" className={classes.content}>
                        {a.text}
                      </Text>
                    </Group>
                  ))}
                </Stack>
              )
            ) : (
              <Stack gap={6}>
                {q.options.map((o) => (
                  <Group key={o.optionId} gap="sm" wrap="nowrap" align="center">
                    <Text size="sm" w={120} style={{ flexShrink: 0 }} truncate="end">
                      {o.label}
                    </Text>
                    <div className={classes.bar} style={{ flex: 1, maxWidth: 240 }}>
                      <div
                        className={`${classes.segment} ${classes.fill}`}
                        style={{ width: `${(o.count * 100) / replied}%` }}
                      />
                    </div>
                    <Text
                      size="sm"
                      fw={600}
                      w={36}
                      className={classes.numeric}
                      data-testid={`option-count-${o.optionId}`}
                    >
                      {o.count}
                    </Text>
                    <Text size="sm" c="dimmed" lineClamp={1}>
                      {o.organizations.join(', ')}
                    </Text>
                  </Group>
                ))}
              </Stack>
            )}
          </Stack>
        ))}
      </Stack>
    </Card>
  );
}
