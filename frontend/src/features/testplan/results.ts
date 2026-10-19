import type { TestResult } from './hooks';

/** 기관 결과 이름. 서버 TestResult 의 라벨과 같다. */
export const RESULT_LABEL: Record<TestResult, string> = {
  PASSED: '정상',
  FAILED: '오류',
  IN_PROGRESS: '수행중',
  NOT_STARTED: '미수행',
};

/** 결과 고르기 선택지. 명세 순서(정상, 오류, 수행중, 미수행)다. */
export const RESULT_OPTIONS = (Object.keys(RESULT_LABEL) as TestResult[]).map((value) => ({
  value,
  label: RESULT_LABEL[value],
}));
