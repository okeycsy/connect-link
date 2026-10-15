import { Group, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

interface Props {
  title: string;
  description?: ReactNode;
  /** 오른쪽 끝의 동작 버튼(추가, 엑셀 등). */
  actions?: ReactNode;
}

/** 목록 화면의 머리. 제목 + 설명 왼쪽, 동작 버튼 오른쪽. 모든 목록 화면(연락처, 의견 요청, 기관 테스트, 일시중단, 관리)이 같은 모양을 쓴다. */
export function ListPageHeader({ title, description, actions }: Props) {
  return (
    <Group justify="space-between" align="flex-end" wrap="nowrap">
      <div>
        <Title order={2}>{title}</Title>
        {description && (
          <Text c="dimmed" size="lg">
            {description}
          </Text>
        )}
      </div>
      {actions && (
        <Group gap="xs" wrap="nowrap">
          {actions}
        </Group>
      )}
    </Group>
  );
}
