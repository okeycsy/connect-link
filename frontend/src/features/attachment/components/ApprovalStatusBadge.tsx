import { Badge } from '@mantine/core';

import type { AttachmentApprovalStatus } from '../hooks';

const LABEL: Record<AttachmentApprovalStatus, string> = {
  NOT_REQUIRED: '결재 없음',
  PENDING: '결재 대기',
  APPROVED: '반출 승인',
  REJECTED: '반려',
};

const COLOR: Record<AttachmentApprovalStatus, string> = {
  NOT_REQUIRED: 'gray',
  PENDING: 'amber',
  APPROVED: 'sky',
  REJECTED: 'red',
};

/** 파일 반출 결재 상태 배지. 결재 대기는 사용자 조치(amber), 승인은 완료(sky), 반려는 red. */
export function ApprovalStatusBadge({ status }: { status: AttachmentApprovalStatus }) {
  return (
    <Badge
      color={COLOR[status]}
      variant={status === 'NOT_REQUIRED' ? 'outline' : 'light'}
      data-testid="approval-status"
    >
      {LABEL[status]}
    </Badge>
  );
}
