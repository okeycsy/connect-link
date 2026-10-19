import { Badge, type BadgeProps } from '@mantine/core';

import type { ContactRank } from '../hooks';

/** 주/부담당. 주담당은 정보(sky), 부담당은 중립(gray outline). */
export function ContactRankBadge({
  rank,
  label,
  ...props
}: { rank: ContactRank; label: string } & BadgeProps) {
  return rank === 'PRIMARY' ? (
    <Badge color="sky" {...props}>
      {label}
    </Badge>
  ) : (
    <Badge color="gray" variant="outline" {...props}>
      {label}
    </Badge>
  );
}

/** 최근 확인일에서 6개월이 지나 확인이 필요하다(주의, amber). */
export function ContactNeedsConfirmBadge(props: BadgeProps) {
  return (
    <Badge color="amber" {...props}>
      확인 필요
    </Badge>
  );
}
