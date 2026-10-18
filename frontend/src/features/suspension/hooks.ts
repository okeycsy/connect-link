import {
  keepPreviousData,
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import type { CurrentUserResponse } from '../account/hooks';
import { historyKeys } from '../history/hooks';
import { homeKeys } from '../home/hooks';

export type SuspensionSummary = components['schemas']['SuspensionSummaryResponse'];
export type SuspensionDetail = components['schemas']['SuspensionDetailResponse'];
export type SuspensionCounts = components['schemas']['SuspensionCountsResponse'];
export type SuspensionSaveRequest = components['schemas']['SuspensionSaveRequest'];
export type SuspensionOrganization = components['schemas']['SuspensionOrganizationResponse'];
export type SuspensionDigest = components['schemas']['SuspensionDigestResponse'];
export type SuspensionStatus = SuspensionSummary['status'];
export type SuspensionPeriod = SuspensionSummary['period'];

export interface SuspensionListParams {
  /** 업무 영역 slug. */
  business: string;
  /** 없으면 전부. */
  period?: SuspensionPeriod;
  /** 결제원 담당자의 기관별 이력 보기. */
  organizationId?: number;
  page?: number;
  size?: number;
}

export const suspensionKeys = {
  all: ['suspensions'] as const,
  list: (params: SuspensionListParams) => ['suspensions', 'list', params] as const,
  counts: (business: string, organizationId?: number) =>
    ['suspensions', 'counts', business, organizationId ?? null] as const,
  detail: (id: number) => ['suspensions', 'detail', id] as const,
  organizations: (business: string) => ['suspensions', 'organizations', business] as const,
};

/** 이 업무의 일시중단을 결제원 담당자로서 다루는지(전 기관 조회, 확인·확정). 서버도 같은 규칙으로 막는다. */
export function canManageSuspensions(me: CurrentUserResponse | undefined, slug: string): boolean {
  if (!me) return false;
  return me.admin || (me.organizationType === 'KFTC' && me.businesses.some((b) => b.slug === slug));
}

/** 이 업무에 일시중단을 등록할 수 있는지. 결제원 담당자와 그 업무를 맡은 참가기관 담당자다. */
export function canRegisterSuspensions(me: CurrentUserResponse | undefined, slug: string): boolean {
  if (!me) return false;
  if (canManageSuspensions(me, slug)) return true;
  return (
    me.organizationType === 'FINANCE_ORGANIZATION' && me.businesses.some((b) => b.slug === slug)
  );
}

export function useSuspensions(params: SuspensionListParams) {
  return useQuery({
    queryKey: suspensionKeys.list(params),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/suspensions', { params: { query: params } })),
    placeholderData: keepPreviousData,
    enabled: Boolean(params.business),
  });
}

export function useSuspensionCounts(business: string, organizationId?: number) {
  return useQuery({
    queryKey: suspensionKeys.counts(business, organizationId),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/suspensions/counts', {
          params: { query: { business, organizationId } },
        }),
      ),
    enabled: Boolean(business),
  });
}

export function useSuspension(id: number | null) {
  return useQuery({
    queryKey: suspensionKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/suspensions/{id}', { params: { path: { id: id! } } })),
    enabled: id != null,
  });
}

/** 등록 양식과 기관별 이력의 기관 목록. 결제원 담당자는 그 업무 참가기관 전부, 기관 담당자는 자기 기관 하나다. */
export function useSuspensionOrganizations(business: string) {
  return useQuery({
    queryKey: suspensionKeys.organizations(business),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/suspensions/organizations', { params: { query: { business } } }),
      ),
    enabled: Boolean(business),
    staleTime: 60_000,
  });
}

/** 일시중단 하나가 바뀐 뒤 다시 읽을 것들. 목록·건수·상세·변경 이력·홈 카드가 함께 바뀐다. */
function invalidate(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: suspensionKeys.all });
  void queryClient.invalidateQueries({ queryKey: historyKeys.all });
  void queryClient.invalidateQueries({ queryKey: homeKeys.all });
}

export function useCreateSuspension() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: SuspensionSaveRequest) =>
      unwrap(await api.POST('/api/v1/suspensions', { body })),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useUpdateSuspension(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: SuspensionSaveRequest) =>
      unwrap(await api.PUT('/api/v1/suspensions/{id}', { params: { path: { id } }, body })),
    onSuccess: (detail) => {
      queryClient.setQueryData(suspensionKeys.detail(id), detail);
      invalidate(queryClient);
    },
  });
}

export function useDeleteSuspension(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.DELETE('/api/v1/suspensions/{id}', { params: { path: { id } } })),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: suspensionKeys.detail(id) });
      invalidate(queryClient);
    },
  });
}

/** 결제원 담당자의 확인 시작. 검토 대기면 검토 중이 된다. */
export function useStartSuspensionReview(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.POST('/api/v1/suspensions/{id}/review', { params: { path: { id } } })),
    onSuccess: (detail) => {
      queryClient.setQueryData(suspensionKeys.detail(id), detail);
      invalidate(queryClient);
    },
  });
}

/** 조치 및 인수인계 사항을 쓰고 확정한다. 확정한 건이면 사항만 고친다. */
export function useConfirmSuspension(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (handover: string) =>
      unwrap(
        await api.POST('/api/v1/suspensions/{id}/confirmation', {
          params: { path: { id } },
          body: { handover },
        }),
      ),
    onSuccess: (detail) => {
      queryClient.setQueryData(suspensionKeys.detail(id), detail);
      invalidate(queryClient);
    },
  });
}

/** 전 영업일 취합을 수동으로 운영·관제 매터모스트 채널에 보낸다. 결제원 담당자만 부른다. */
export function useSendSuspensionDigest() {
  return useMutation({
    mutationFn: async () => unwrap(await api.POST('/api/v1/suspension-digests')),
  });
}
