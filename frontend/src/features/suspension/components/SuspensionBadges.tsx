import { Badge } from '@mantine/core';

import type { SuspensionPeriod, SuspensionStatus } from '../hooks';

const STATUS_COLOR: Record<SuspensionStatus, string> = {
  PENDING_REVIEW: 'amber',
  IN_REVIEW: 'clay',
  CONFIRMED: 'sky',
  COMPLETED: 'sky',
};

const STATUS_LABEL: Record<SuspensionStatus, string> = {
  PENDING_REVIEW: '검토 대기',
  IN_REVIEW: '검토 중',
  CONFIRMED: '확정',
  COMPLETED: '완료',
};

/**
 * 일시중단 상태 배지. 검토 대기는 결제원 확인이 필요해 amber, 검토 중은 결제원 내부 흐름이라 clay, 확정·완료는 sky(frontend/CLAUDE.md 2.3).
 */
export function SuspensionStatusBadge({
  status,
  size = 'sm',
}: {
  status: SuspensionStatus;
  size?: 'xs' | 'sm' | 'md';
}) {
  return (
    <Badge size={size} color={STATUS_COLOR[status]} data-testid="suspension-status">
      {STATUS_LABEL[status]}
    </Badge>
  );
}

/** 지금 중단 중인 건에만 붙는 배지(주의, amber). 예정·지난 건은 배지 없이 둔다. */
export function SuspensionPeriodBadge({
  period,
  size = 'sm',
}: {
  period: SuspensionPeriod;
  size?: 'xs' | 'sm' | 'md';
}) {
  if (period !== 'CURRENT') return null;
  return (
    <Badge size={size} color="amber">
      중단 중
    </Badge>
  );
}
