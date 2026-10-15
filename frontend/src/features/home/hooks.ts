import { useQuery } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';

export type HomeResponse = components['schemas']['HomeResponse'];
export type HomeCard = components['schemas']['HomeCard'];
export type HomeItem = components['schemas']['HomeCardItem'];
export type HomeKpi = components['schemas']['HomeCardKpi'];

export const homeKeys = {
  summary: (businessSlug: string) => ['home', businessSlug] as const,
};

/** 레일에서 고른 업무 영역의 홈(KPI + 할 일 카드). */
export function useHome(businessSlug: string | undefined) {
  return useQuery({
    queryKey: homeKeys.summary(businessSlug ?? ''),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/home', { params: { query: { business: businessSlug! } } })),
    enabled: Boolean(businessSlug),
  });
}
