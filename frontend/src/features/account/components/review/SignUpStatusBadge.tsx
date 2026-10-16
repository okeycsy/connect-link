import { Badge } from '@mantine/core';

import type { SignUpApplicationResponse } from '../../hooks';

/** 가입 신청 상태 배지. 승인 대기는 사용자 조치가 필요하므로 amber, 승인은 sky, 반려는 red(frontend/CLAUDE.md 2.3). */
export function SignUpStatusBadge({
  application,
  size = 'sm',
}: {
  application: Pick<SignUpApplicationResponse, 'status'>;
  size?: 'xs' | 'sm' | 'md';
}) {
  switch (application.status) {
    case 'PENDING_APPROVAL':
      return (
        <Badge size={size} color="amber">
          승인 대기
        </Badge>
      );
    case 'ACTIVE':
      return (
        <Badge size={size} color="sky">
          승인
        </Badge>
      );
    case 'REJECTED':
      return (
        <Badge size={size} color="red">
          반려
        </Badge>
      );
    default:
      return (
        <Badge size={size} variant="outline" color="gray">
          탈퇴
        </Badge>
      );
  }
}
