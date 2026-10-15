import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';

export type SearchResponse = components['schemas']['SearchResponse'];
export type SearchGroup = components['schemas']['SearchGroup'];
export type SearchHit = components['schemas']['SearchHit'];

export const searchKeys = {
  query: (business: string | undefined, q: string) => ['search', business, q] as const,
};

/** 전역 검색. 검색어가 비면 부르지 않고, 글자를 더 치는 동안 이전 결과를 유지해 목록이 깜빡이지 않게 한다. */
export function useSearch(business: string | undefined, q: string) {
  return useQuery({
    queryKey: searchKeys.query(business, q),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/search', { params: { query: { business: business!, q } } })),
    enabled: !!business && q.length > 0,
    placeholderData: keepPreviousData,
    staleTime: 10_000,
  });
}
