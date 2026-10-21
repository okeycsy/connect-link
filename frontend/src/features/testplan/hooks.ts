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

export type TestPlanSummary = components['schemas']['TestPlanSummaryResponse'];
export type TestPlanDetail = components['schemas']['TestPlanDetailResponse'];
export type TestPlanCounts = components['schemas']['TestPlanCountsResponse'];
export type TestPlanSaveRequest = components['schemas']['TestPlanSaveRequest'];
export type TestPlanOrganization = components['schemas']['TestPlanOrganizationResponse'];
export type TestTargetSummary = components['schemas']['TestTargetSummaryResponse'];
export type TestTargetResults = components['schemas']['TestTargetResultsResponse'];
export type TestItemResultRow = components['schemas']['TestItemResultResponse'];
export type TestResultSaveRequest = components['schemas']['TestResultSaveRequest'];
export type TestFeedbackRequest = components['schemas']['TestFeedbackRequest'];
export type TestData = components['schemas']['TestDataResponse'];
export type TestDataSaveRequest = components['schemas']['TestDataSaveRequest'];
export type TestDataStatus = TestDataSaveRequest['status'];
export type TestPlanRecipient = components['schemas']['TestPlanRecipientResponse'];
export type TestPlanState = TestPlanSummary['state'];
export type TestResult = TestResultSaveRequest['results'][number]['result'];

export interface TestPlanListParams {
  /** 업무 영역 slug. */
  business: string;
  /** 없으면 전부. */
  state?: TestPlanState;
  page?: number;
  size?: number;
}

export const testPlanKeys = {
  all: ['test-plans'] as const,
  list: (params: TestPlanListParams) => ['test-plans', 'list', params] as const,
  counts: (business: string) => ['test-plans', 'counts', business] as const,
  detail: (id: number) => ['test-plans', 'detail', id] as const,
  target: (id: number, organizationId: number) =>
    ['test-plans', 'target', id, organizationId] as const,
  testData: (id: number, organizationId: number) =>
    ['test-plans', 'test-data', id, organizationId] as const,
  organizations: (business: string) => ['test-plans', 'organizations', business] as const,
  recipients: (id: number) => ['test-plans', 'recipients', id] as const,
};

/**
 * 이 업무의 기관 테스트를 결제원 담당자로서 다루는지(만들기·고치기, 전 기관 결과, 의견, 알림 설정). 운영·개발·분석·사업부서 담당자의
 * 권한은 같다. 서버도 같은 규칙으로 막는다.
 */
export function canManageTestPlans(me: CurrentUserResponse | undefined, slug: string): boolean {
  if (!me) return false;
  return me.admin || (me.organizationType === 'KFTC' && me.businesses.some((b) => b.slug === slug));
}

export function useTestPlans(params: TestPlanListParams) {
  return useQuery({
    queryKey: testPlanKeys.list(params),
    queryFn: async () => unwrap(await api.GET('/api/v1/test-plans', { params: { query: params } })),
    placeholderData: keepPreviousData,
    enabled: Boolean(params.business),
  });
}

export function useTestPlanCounts(business: string) {
  return useQuery({
    queryKey: testPlanKeys.counts(business),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/test-plans/counts', { params: { query: { business } } })),
    enabled: Boolean(business),
  });
}

export function useTestPlan(id: number | null) {
  return useQuery({
    queryKey: testPlanKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/test-plans/{id}', { params: { path: { id: id! } } })),
    enabled: id != null,
  });
}

/** 대상 기관 후보(그 업무에 참여하는 참가기관). 결제원 담당자만 부른다. */
export function useTestPlanOrganizations(business: string) {
  return useQuery({
    queryKey: testPlanKeys.organizations(business),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/test-plans/organizations', { params: { query: { business } } }),
      ),
    enabled: Boolean(business),
    staleTime: 60_000,
  });
}

/** 대상 기관 한 곳의 요구사항별 결과와 결제원 의견. */
export function useTestTargetResults(id: number, organizationId: number | null) {
  return useQuery({
    queryKey: testPlanKeys.target(id, organizationId ?? 0),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/test-plans/{id}/targets/{organizationId}', {
          params: { path: { id, organizationId: organizationId! } },
        }),
      ),
    enabled: organizationId != null,
  });
}

/** 결과 등록 알림 대상 후보와 지금 고른 사람. 결제원 담당자만 부른다. */
export function useTestPlanRecipients(id: number | null) {
  return useQuery({
    queryKey: testPlanKeys.recipients(id ?? 0),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/test-plans/{id}/recipients', { params: { path: { id: id! } } }),
      ),
    enabled: id != null,
  });
}

/** 기관 테스트가 바뀐 뒤 다시 읽을 것들. 목록·건수·상세·기관 결과·변경 이력·홈 카드가 함께 바뀐다. */
function invalidate(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: testPlanKeys.all });
  void queryClient.invalidateQueries({ queryKey: historyKeys.all });
  void queryClient.invalidateQueries({ queryKey: homeKeys.all });
}

export function useCreateTestPlan() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: TestPlanSaveRequest) =>
      unwrap(await api.POST('/api/v1/test-plans', { body })),
    onSuccess: () => invalidate(queryClient),
  });
}

export function useUpdateTestPlan(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: TestPlanSaveRequest) =>
      unwrap(await api.PUT('/api/v1/test-plans/{id}', { params: { path: { id } }, body })),
    onSuccess: (detail) => {
      queryClient.setQueryData(testPlanKeys.detail(id), detail);
      invalidate(queryClient);
    },
  });
}

export function useDeleteTestPlan(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.DELETE('/api/v1/test-plans/{id}', { params: { path: { id } } })),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: testPlanKeys.detail(id) });
      invalidate(queryClient);
    },
  });
}

/** 기관 담당자가 자기 기관 결과를 저장한다. 바뀐 행만 보낸다. */
export function useSaveTestResults(id: number, organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: TestResultSaveRequest) =>
      unwrap(
        await api.PUT('/api/v1/test-plans/{id}/targets/{organizationId}/results', {
          params: { path: { id, organizationId } },
          body,
        }),
      ),
    onSuccess: (results) => {
      queryClient.setQueryData(testPlanKeys.target(id, organizationId), results);
      invalidate(queryClient);
    },
  });
}

/** 결제원 담당자가 기관 결과 하나에 의견(미니 수행률·특이사항)을 남긴다. 둘 다 비우면 지운다. */
export function useSaveTestFeedback(id: number, organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ itemId, ...body }: TestFeedbackRequest & { itemId: number }) =>
      unwrap(
        await api.PUT('/api/v1/test-plans/{id}/targets/{organizationId}/items/{itemId}/feedback', {
          params: { path: { id, organizationId, itemId } },
          body,
        }),
      ),
    onSuccess: (results) => {
      queryClient.setQueryData(testPlanKeys.target(id, organizationId), results);
      invalidate(queryClient);
    },
  });
}

/** 대상 기관 한 곳의 테스트 전 테스트데이터 생성 여부·메모·파일 수. */
export function useTestData(id: number, organizationId: number) {
  return useQuery({
    queryKey: testPlanKeys.testData(id, organizationId),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/test-plans/{id}/targets/{organizationId}/test-data', {
          params: { path: { id, organizationId } },
        }),
      ),
  });
}

/** 기관 담당자가 자기 기관 테스트데이터 생성 여부와 메모를 저장한다. */
export function useSaveTestData(id: number, organizationId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: TestDataSaveRequest) =>
      unwrap(
        await api.PUT('/api/v1/test-plans/{id}/targets/{organizationId}/test-data', {
          params: { path: { id, organizationId } },
          body,
        }),
      ),
    onSuccess: (data) => {
      queryClient.setQueryData(testPlanKeys.testData(id, organizationId), data);
      invalidate(queryClient);
    },
  });
}

export function useUpdateTestPlanRecipients(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (accountIds: number[]) =>
      unwrap(
        await api.PUT('/api/v1/test-plans/{id}/recipients', {
          params: { path: { id } },
          body: { accountIds },
        }),
      ),
    onSuccess: (recipients) => {
      queryClient.setQueryData(testPlanKeys.recipients(id), recipients);
      invalidate(queryClient);
    },
  });
}
