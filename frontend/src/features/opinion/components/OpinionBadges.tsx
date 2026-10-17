import { Badge } from '@mantine/core';

import type { OpinionReplyStatus, OpinionRequestStatus } from '../hooks';

/** 요청 상태 배지. 회신중은 기관 조치가 필요해 amber, 마감은 sky, 작성중은 결제원 내부 흐름이라 clay, 취소는 중립(frontend/CLAUDE.md 2.3). */
export function OpinionStatusBadge({
  status,
  cancelled,
  size = 'sm',
}: {
  status: OpinionRequestStatus;
  cancelled: boolean;
  size?: 'xs' | 'sm' | 'md';
}) {
  if (cancelled) {
    return (
      <Badge size={size} variant="outline" color="gray">
        취소
      </Badge>
    );
  }
  switch (status) {
    case 'DRAFT':
      return (
        <Badge size={size} color="clay">
          작성중
        </Badge>
      );
    case 'OPEN':
      return (
        <Badge size={size} color="amber">
          회신중
        </Badge>
      );
    default:
      return (
        <Badge size={size} color="sky">
          마감
        </Badge>
      );
  }
}

/** 회신 기한 배지. 회신중일 때만 남은 날을 보인다. 지나면 빨강 채움, 3일 안이면 amber. */
export function DueBadge({
  status,
  daysLeft,
  size = 'sm',
}: {
  status: OpinionRequestStatus;
  daysLeft: number;
  size?: 'xs' | 'sm' | 'md';
}) {
  if (status !== 'OPEN') return null;
  if (daysLeft < 0) {
    return (
      <Badge size={size} color="red" variant="filled">
        기한 {-daysLeft}일 지남
      </Badge>
    );
  }
  return (
    <Badge size={size} color={daysLeft <= 3 ? 'amber' : 'sky'}>
      {daysLeft === 0 ? 'D-DAY' : `D-${daysLeft}`}
    </Badge>
  );
}

/** 기관 회신 상태 배지. 아직 없으면 "미작성"(중립), 임시저장·수정 중은 기관 조치가 남아 amber, 수정 요청은 결제원 검토라 clay. */
export function ReplyStatusBadge({
  status,
  size = 'sm',
}: {
  status: OpinionReplyStatus | null | undefined;
  size?: 'xs' | 'sm' | 'md';
}) {
  switch (status) {
    case 'SUBMITTED':
      return (
        <Badge size={size} color="sky">
          제출
        </Badge>
      );
    case 'DRAFT':
      return (
        <Badge size={size} color="amber">
          임시저장
        </Badge>
      );
    case 'REVISION_REQUESTED':
      return (
        <Badge size={size} color="clay">
          수정 요청
        </Badge>
      );
    case 'REVISING':
      return (
        <Badge size={size} color="amber">
          수정 중
        </Badge>
      );
    default:
      return (
        <Badge size={size} variant="outline" color="gray">
          미작성
        </Badge>
      );
  }
}
