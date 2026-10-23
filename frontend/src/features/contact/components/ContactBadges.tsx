import { Badge, type BadgeProps, Tooltip } from '@mantine/core';
import { IconPhoneOff } from '@tabler/icons-react';

import type { ContactRank, ContactUnreachable } from '../hooks';
import { unreachableSummary } from '../unreachable';

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

/** 전화가 닿지 않아 결제원이 남긴 연락 불가 표시(긴급, red). 올리면 언제·누가·메모를 보인다. */
export function ContactUnreachableBadge({
  unreachable,
  ...props
}: { unreachable: ContactUnreachable } & BadgeProps) {
  return (
    <Tooltip
      label={unreachableSummary(unreachable)}
      color="dark"
      multiline
      maw={280}
      withArrow
      events={{ hover: true, focus: true, touch: true }}
    >
      <Badge
        color="red"
        leftSection={<IconPhoneOff size={11} stroke={2} />}
        data-testid="contact-unreachable-badge"
        {...props}
      >
        연락 불가
      </Badge>
    </Tooltip>
  );
}
