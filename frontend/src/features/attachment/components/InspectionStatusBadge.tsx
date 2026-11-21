import { Badge } from '@mantine/core';

import type { AttachmentInspectionStatus } from '../hooks';

const LABEL: Record<AttachmentInspectionStatus, string> = {
  NOT_REQUIRED: '검사 없음',
  PENDING: '보안 검사 중',
  PASSED: '검사 완료',
  BLOCKED: '반입 차단',
};

const COLOR: Record<AttachmentInspectionStatus, string> = {
  NOT_REQUIRED: 'gray',
  PENDING: 'amber',
  PASSED: 'sky',
  BLOCKED: 'red',
};

/**
 * 반입 보안 검사 상태 배지. 참가기관이 올린 파일은 검사를 통과해야 내려받을 수 있다. 검사 중은 기다리는 중(amber), 통과는
 * 완료(sky), 차단은 red.
 */
export function InspectionStatusBadge({ status }: { status: AttachmentInspectionStatus }) {
  return (
    <Badge color={COLOR[status]} variant="light" data-testid="inspection-status">
      {LABEL[status]}
    </Badge>
  );
}
