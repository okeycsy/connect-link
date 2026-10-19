import { Badge } from '@mantine/core';

import type { TestPlanState, TestResult } from '../hooks';
import { RESULT_LABEL } from '../results';

type Size = 'xs' | 'sm' | 'md';

const STATE_LABEL: Record<TestPlanState, string> = {
  IN_PROGRESS: '진행 중',
  COMPLETED: '수행 완료',
};

/** 기관 테스트 진행 구분 배지. 진행 중·수행 완료 모두 정보·진행·완료 색(sky)이다(frontend/CLAUDE.md 2.3). */
export function TestPlanStateBadge({ state, size = 'sm' }: { state: TestPlanState; size?: Size }) {
  return (
    <Badge size={size} color="sky" data-testid="test-plan-state">
      {STATE_LABEL[state]}
    </Badge>
  );
}

/** 수행 기간이 지났는데 완료가 아닌 테스트. 초과라 빨강 채움이다. */
export function OverdueBadge({ size = 'sm' }: { size?: Size }) {
  return (
    <Badge size={size} color="red" variant="filled" style={{ flexShrink: 0 }}>
      기간 지남
    </Badge>
  );
}

const RESULT_COLOR: Record<TestResult, string> = {
  PASSED: 'sky',
  FAILED: 'red',
  IN_PROGRESS: 'sky',
  NOT_STARTED: 'gray',
};

/** 기관이 등록한 결과 배지. 정상·수행중 sky, 오류 red, 미수행 gray 테두리, 아직 등록하지 않았으면 "미등록" gray 테두리. */
export function TestResultBadge({
  result,
  size = 'sm',
}: {
  result: TestResult | null | undefined;
  size?: Size;
}) {
  if (!result) {
    return (
      <Badge size={size} color="gray" variant="outline">
        미등록
      </Badge>
    );
  }
  return (
    <Badge
      size={size}
      color={RESULT_COLOR[result]}
      variant={result === 'NOT_STARTED' ? 'outline' : 'light'}
      data-testid="test-result"
    >
      {RESULT_LABEL[result]}
    </Badge>
  );
}
