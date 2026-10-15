import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';

import { api, DEMO_NETWORK_ZONE_KEY, unwrap } from '../api/client';

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

export type DemoNetworkZone = 'INTERNAL' | 'EXTERNAL';

function readDemoZone(): DemoNetworkZone {
  try {
    return window.localStorage.getItem(DEMO_NETWORK_ZONE_KEY) === 'EXTERNAL'
      ? 'EXTERNAL'
      : 'INTERNAL';
  } catch {
    return 'INTERNAL';
  }
}

/**
 * 데모 도구의 망 전환. 값은 이 브라우저에만 남고(localStorage) 모든 API 요청에 X-Network-Zone 헤더로 실린다.
 * 바꾸면 서버 응답이 달라지므로 캐시를 전부 다시 읽는다.
 */
export function useDemoNetworkZone(): [DemoNetworkZone, (zone: DemoNetworkZone) => void] {
  const queryClient = useQueryClient();
  const [zone, setZoneState] = useState<DemoNetworkZone>(readDemoZone);
  const setZone = useCallback(
    (next: DemoNetworkZone) => {
      try {
        if (next === 'INTERNAL') window.localStorage.removeItem(DEMO_NETWORK_ZONE_KEY);
        else window.localStorage.setItem(DEMO_NETWORK_ZONE_KEY, next);
      } catch {
        // 저장 실패는 무시한다. 이번 화면에서만 바뀐다.
      }
      setZoneState(next);
      void queryClient.invalidateQueries();
    },
    [queryClient],
  );
  return [zone, setZone];
}
