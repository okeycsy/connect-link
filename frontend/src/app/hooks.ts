import { useQuery } from '@tanstack/react-query';

import { api, unwrap } from '../api/client';

/** 서버 상태와 프로필. 상단 바의 데모 도구는 프로필이 demo 일 때만 보인다. */
export function useServerInfo() {
  return useQuery({
    queryKey: ['server-info'],
    queryFn: async () => unwrap(await api.GET('/api/v1/health')),
    staleTime: Infinity,
  });
}

export function isDemoProfile(profile: string | undefined): boolean {
  return profile?.split(',').includes('demo') ?? false;
}
