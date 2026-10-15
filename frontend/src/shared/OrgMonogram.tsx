import { Avatar } from '@mantine/core';

interface Props {
  name: string;
  /** 기관 구분. 결제원은 남색 채움, 참가기관은 남색 틴트, 이용기관은 테두리만. */
  type: 'KFTC' | 'FINANCE_ORGANIZATION' | 'USE_ORGANIZATION';
  size?: number;
}

const VARIANT = {
  KFTC: 'filled',
  FINANCE_ORGANIZATION: 'light',
  USE_ORGANIZATION: 'outline',
} as const;

/** 기관 모노그램. 목록의 기관 열과 상세 패널 머리에서 기관을 한눈에 구분하는 글자 하나. */
export function OrgMonogram({ name, type, size = 32 }: Props) {
  return (
    <Avatar radius="md" size={size} color="navy" variant={VARIANT[type]} aria-hidden>
      {name.slice(0, 1)}
    </Avatar>
  );
}
