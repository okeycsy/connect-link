import { useQuery } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';

export type CurrentUserResponse = components['schemas']['CurrentUserResponse'];

export const accountKeys = {
  me: ['me'] as const,
};

/** 로그인 사용자. 계정 모듈 전까지 demo 프로필은 서버가 데모 관리자로 인증해 준다. */
export function useMe() {
  return useQuery({
    queryKey: accountKeys.me,
    queryFn: async () => unwrap(await api.GET('/api/v1/me')),
    staleTime: 5 * 60_000,
  });
}
