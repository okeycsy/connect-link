import { ApiError, errorCodeOf } from '../../api/client';

export type StateKind = 'empty' | 'error' | 'forbidden' | 'notFound' | 'internalOnly';

/** API 오류를 상태 화면 종류로 바꾼다. 403 중 INTERNAL_ONLY 는 내부망 전용, 나머지 403 은 권한 없음. */
export function stateKindOf(error: unknown): StateKind {
  if (error instanceof ApiError) {
    if (error.status === 404) return 'notFound';
    if (error.status === 403) {
      return errorCodeOf(error) === 'INTERNAL_ONLY' ? 'internalOnly' : 'forbidden';
    }
  }
  return 'error';
}
