import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';

export type BusinessResponse = components['schemas']['BusinessResponse'];
export type OrganizationResponse = components['schemas']['OrganizationResponse'];

export const organizationKeys = {
  businesses: ['businesses'] as const,
  organizations: ['organizations'] as const,
};

/** 업무 영역 목록. 레일과 가입 화면이 쓴다. 바뀌지 않는 기준정보라 길게 캐시한다. */
export function useBusinesses() {
  return useQuery({
    queryKey: organizationKeys.businesses,
    queryFn: async () => unwrap(await api.GET('/api/v1/businesses')),
    staleTime: Infinity,
  });
}

/** 사용 중인 기관 목록. */
export function useOrganizations() {
  return useQuery({
    queryKey: organizationKeys.organizations,
    queryFn: async () => unwrap(await api.GET('/api/v1/organizations')),
  });
}

/** 주소의 업무 영역 slug 에 해당하는 업무. */
export function useCurrentBusiness(): BusinessResponse | undefined {
  const { business } = useParams<{ business: string }>();
  const { data } = useBusinesses();
  return data?.find((b) => b.slug === business);
}
