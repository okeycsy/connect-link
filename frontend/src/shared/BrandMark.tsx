import { Group, Text } from '@mantine/core';

interface Props {
  /** 표시 크기(높이 px). 상단 바 24, 로그인 32. */
  size?: number;
  /** 글자 크기. */
  textSize?: string;
}

/** 연결고리 로고: 오른쪽이 뾰족한 클레이 선 오각형 + "연결고리". 상단 바와 로그인에서만 쓴다. */
export function BrandMark({ size = 24, textSize = '15px' }: Props) {
  return (
    <Group gap={10} wrap="nowrap">
      <svg aria-hidden width={(size * 124) / 84} height={size} viewBox="0 0 124 84">
        <path
          d="M2 2 H92 L120 42 L92 82 H2 Z"
          fill="none"
          stroke="var(--mantine-color-clay-6)"
          strokeWidth={size >= 32 ? 5 : 7}
        />
      </svg>
      <Text fw={700} fz={textSize} lts="0.02em">
        연결고리
      </Text>
    </Group>
  );
}
