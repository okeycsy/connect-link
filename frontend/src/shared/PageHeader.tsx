import { Group, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

import classes from './PageHeader.module.css';

interface Props {
  title: string;
  description?: ReactNode;
  /** 제목 오른쪽의 상태 배지(회신 중, 검토 중 등). */
  status?: ReactNode;
  /** 오른쪽 끝의 동작 버튼(추가, 엑셀 등). 채움 버튼은 하나만 둔다. */
  actions?: ReactNode;
}

/**
 * 화면 머리 막대. 본문 위 끝에서 끝까지 붙는 흰 막대(아래 1px 선)에 제목 + 설명, 오른쪽에 동작 버튼.
 * 홈을 뺀 모든 화면(목록, 관리, 알림, 내 정보, 준비 중 자리)이 같은 모양을 쓴다. frontend/CLAUDE.md "화면 머리 막대".
 */
export function PageHeader({ title, description, status, actions }: Props) {
  return (
    <Group className={classes.bar} justify="space-between" align="center" gap="md">
      <div className={classes.text}>
        <Group gap="sm">
          <Title order={1} className={classes.title}>
            {title}
          </Title>
          {status}
        </Group>
        {description && (
          <Text c="dimmed" size="md">
            {description}
          </Text>
        )}
      </div>
      {actions && (
        <Group gap="xs" className={classes.actions}>
          {actions}
        </Group>
      )}
    </Group>
  );
}
