import { Avatar } from '@mantine/core';

interface Props {
  name: string;
  /** 기관 구분. 결제원은 스카이, 참가기관은 클레이, 이용기관은 회색 테두리. */
  type: 'KFTC' | 'FINANCE_ORGANIZATION' | 'USE_ORGANIZATION';
  size?: number;
}

const STYLE = {
  KFTC: { color: 'sky', variant: 'light' },
  FINANCE_ORGANIZATION: { color: 'clay', variant: 'light' },
  USE_ORGANIZATION: { color: 'gray', variant: 'outline' },
} as const;

/** 기관 모노그램. 목록의 기관 열과 상세 패널 머리에서 기관을 한눈에 구분하는 글자 하나(둥근 사각 radius 10). */
export function OrgMonogram({ name, type, size = 32 }: Props) {
  return (
    <Avatar radius={10} size={size} {...STYLE[type]} aria-hidden>
      {name.slice(0, 1)}
    </Avatar>
  );
}
