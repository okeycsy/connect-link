import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import { historyKeys } from '../history/hooks';

export type BusinessResponse = components['schemas']['BusinessResponse'];
export type OrganizationResponse = components['schemas']['OrganizationResponse'];
export type OrganizationDetail = components['schemas']['OrganizationDetailResponse'];
export type OrganizationCreateRequest = components['schemas']['OrganizationCreateRequest'];
export type OrganizationUpdateRequest = components['schemas']['OrganizationUpdateRequest'];
export type OrganizationType = OrganizationDetail['type'];
export type BusinessCode = BusinessResponse['code'];
export type JobRoleResponse = components['schemas']['JobRoleResponse'];

export interface OrganizationSearchParams {
  keyword?: string;
  type?: OrganizationType;
  /** 업무 영역 slug. 비우면 전 업무. */
  business?: string;
  active?: boolean;
  page?: number;
  size?: number;
}

export const organizationKeys = {
  businesses: ['businesses'] as const,
  jobRoles: ['job-roles'] as const,
  organizations: ['organizations'] as const,
  search: (params: OrganizationSearchParams) => ['organizations', 'search', params] as const,
  detail: (id: number) => ['organizations', 'detail', id] as const,
};

/** 업무 영역 목록. 레일과 가입 화면이 쓴다. 바뀌지 않는 기준정보라 길게 캐시한다. */
export function useBusinesses() {
  return useQuery({
    queryKey: organizationKeys.businesses,
    queryFn: async () => unwrap(await api.GET('/api/v1/businesses')),
    staleTime: Infinity,
  });
}

/** 직무와 전산 세부 직무 선택지(공개). 가입 화면이 쓴다. */
export function useJobRoles() {
  return useQuery({
    queryKey: organizationKeys.jobRoles,
    queryFn: async () => unwrap(await api.GET('/api/v1/job-roles')),
    staleTime: Infinity,
  });
}

/** 사용 중인 기관 목록(공개). */
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

/** 관리 화면의 기관 검색. 필터를 바꿔도 이전 결과를 잠시 보여 줘 표가 깜빡이지 않게 한다. */
export function useOrganizationSearch(params: OrganizationSearchParams) {
  return useQuery({
    queryKey: organizationKeys.search(params),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/organizations/search', { params: { query: params } })),
    placeholderData: keepPreviousData,
  });
}

export function useOrganization(id: number | null) {
  return useQuery({
    queryKey: organizationKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/organizations/{id}', { params: { path: { id: id! } } })),
    enabled: id != null,
  });
}

export function useCreateOrganization() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: OrganizationCreateRequest) =>
      unwrap(await api.POST('/api/v1/organizations', { body })),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: organizationKeys.organizations });
      queryClient.invalidateQueries({ queryKey: historyKeys.all });
    },
  });
}

export function useUpdateOrganization() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: OrganizationUpdateRequest }) =>
      unwrap(await api.PUT('/api/v1/organizations/{id}', { params: { path: { id } }, body })),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: organizationKeys.organizations });
      // 저장하면 변경 이력이 늘어난다.
      queryClient.invalidateQueries({ queryKey: historyKeys.all });
    },
  });
}
