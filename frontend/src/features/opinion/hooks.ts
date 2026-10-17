import {
  keepPreviousData,
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api, ApiError, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import type { CurrentUserResponse } from '../account/hooks';
import { attachmentKeys } from '../attachment/hooks';
import { historyKeys } from '../history/hooks';
import { homeKeys } from '../home/hooks';

export type OpinionRequestSummary = components['schemas']['OpinionRequestSummaryResponse'];
export type OpinionRequestDetail = components['schemas']['OpinionRequestDetailResponse'];
export type OpinionRequestCounts = components['schemas']['OpinionRequestCountsResponse'];
export type OpinionRequestCreateRequest = components['schemas']['OpinionRequestCreateRequest'];
export type OpinionRequestUpdateRequest = components['schemas']['OpinionRequestUpdateRequest'];
export type OpinionTarget = components['schemas']['OpinionTargetResponse'];
export type OpinionQuestion = components['schemas']['OpinionQuestionResponse'];
export type OpinionQuestionRequest = components['schemas']['OpinionQuestionRequest'];
export type OpinionQuestionType = OpinionQuestion['type'];
export type OpinionAnswer = components['schemas']['OpinionAnswerResponse'];
export type OpinionAnswerRequest = components['schemas']['OpinionAnswerRequest'];
export type OpinionRecipientCandidate = components['schemas']['OpinionRecipientCandidateResponse'];
export type OpinionRecipientCandidateMember =
  components['schemas']['OpinionRecipientCandidateMember'];
export type OpinionMyReply = components['schemas']['OpinionMyReplyResponse'];
export type OpinionReplySaveRequest = components['schemas']['OpinionReplySaveRequest'];
export type OpinionCollation = components['schemas']['OpinionCollationResponse'];
export type OpinionCollationRow = components['schemas']['OpinionCollationRow'];
export type OpinionReminder = components['schemas']['OpinionReminderResponse'];
export type OpinionRequestStatus = OpinionRequestSummary['status'];
export type OpinionReplyType = NonNullable<OpinionReplySaveRequest['type']>;
export type OpinionReplyStatus = NonNullable<OpinionMyReply['status']>;

export interface OpinionRequestListParams {
  /** 업무 영역 slug. */
  business: string;
  status: OpinionRequestStatus;
  page?: number;
  size?: number;
}

export const opinionKeys = {
  all: ['opinion-requests'] as const,
  list: (params: OpinionRequestListParams) => ['opinion-requests', 'list', params] as const,
  counts: (business: string) => ['opinion-requests', 'counts', business] as const,
  detail: (id: number) => ['opinion-requests', 'detail', id] as const,
  candidates: (business: string) => ['opinion-requests', 'candidates', business] as const,
  myReply: (id: number) => ['opinion-requests', 'my-reply', id] as const,
  collation: (id: number) => ['opinion-requests', 'collation', id] as const,
  reminders: (id: number) => ['opinion-requests', 'reminders', id] as const,
};

/** 의견 유형 이름. 서버 라벨과 같다. 선택지와 차트 범례가 같은 순서(동의, 조건부 동의, 이견)를 쓴다. */
export const REPLY_TYPE_OPTIONS: { value: OpinionReplyType; label: string }[] = [
  { value: 'AGREE', label: '동의' },
  { value: 'CONDITIONAL', label: '조건부 동의' },
  { value: 'DISAGREE', label: '이견' },
];

/** 이 업무의 의견 요청을 만들고 관리할 수 있는지. 관리자와 그 업무의 결제원 담당자다(서버도 같은 규칙으로 막는다). */
export function canManageOpinions(me: CurrentUserResponse | undefined, slug: string): boolean {
  if (!me) return false;
  return me.admin || (me.organizationType === 'KFTC' && me.businesses.some((b) => b.slug === slug));
}

export function useOpinionRequests(params: OpinionRequestListParams) {
  return useQuery({
    queryKey: opinionKeys.list(params),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/opinion-requests', { params: { query: params } })),
    placeholderData: keepPreviousData,
    enabled: Boolean(params.business),
  });
}

export function useOpinionRequestCounts(business: string) {
  return useQuery({
    queryKey: opinionKeys.counts(business),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/opinion-requests/counts', { params: { query: { business } } })),
    enabled: Boolean(business),
  });
}

export function useOpinionRequest(id: number | null) {
  return useQuery({
    queryKey: opinionKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/opinion-requests/{id}', { params: { path: { id: id! } } })),
    enabled: id != null,
  });
}

/** 대상 기관과 회신 담당자 후보. 작성 화면에서만 쓴다(결제원 담당자 전용). */
export function useRecipientCandidates(business: string) {
  return useQuery({
    queryKey: opinionKeys.candidates(business),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/opinion-requests/recipient-candidates', {
          params: { query: { business } },
        }),
      ),
    enabled: Boolean(business),
    staleTime: 60_000,
  });
}

/** 요청 하나가 바뀐 뒤 다시 읽을 것들. 목록·건수·상세·회신 현황·변경 이력·홈 카드가 함께 바뀐다. */
function invalidateRequest(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: opinionKeys.all });
  void queryClient.invalidateQueries({ queryKey: historyKeys.all });
  void queryClient.invalidateQueries({ queryKey: homeKeys.all });
}

export function useCreateOpinionRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: OpinionRequestCreateRequest) =>
      unwrap(await api.POST('/api/v1/opinion-requests', { body })),
    onSuccess: () => invalidateRequest(queryClient),
  });
}

export function useUpdateOpinionRequest(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: OpinionRequestUpdateRequest) =>
      unwrap(await api.PUT('/api/v1/opinion-requests/{id}', { params: { path: { id } }, body })),
    onSuccess: (detail) => {
      queryClient.setQueryData(opinionKeys.detail(id), detail);
      invalidateRequest(queryClient);
    },
  });
}

/** 회신 요청 시작(작성중 → 회신중). 대상 기관 회신 담당자에게 메일이 간다. */
export function useOpenOpinionRequest(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.POST('/api/v1/opinion-requests/{id}/opening', { params: { path: { id } } })),
    onSuccess: (detail) => {
      queryClient.setQueryData(opinionKeys.detail(id), detail);
      invalidateRequest(queryClient);
    },
  });
}

export function useCloseOpinionRequest(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () =>
      unwrap(await api.POST('/api/v1/opinion-requests/{id}/closing', { params: { path: { id } } })),
    onSuccess: (detail) => {
      queryClient.setQueryData(opinionKeys.detail(id), detail);
      invalidateRequest(queryClient);
    },
  });
}

/** 회신중 요청 취소. 사유는 대상 기관에 가는 취소 메일에 들어간다. */
export function useCancelOpinionRequest(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (reason: string) =>
      unwrap(
        await api.POST('/api/v1/opinion-requests/{id}/cancellation', {
          params: { path: { id } },
          body: { reason },
        }),
      ),
    onSuccess: (detail) => {
      queryClient.setQueryData(opinionKeys.detail(id), detail);
      invalidateRequest(queryClient);
    },
  });
}

/** 작성중 요청 삭제. */
export function useDeleteOpinionRequest(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const result = await api.DELETE('/api/v1/opinion-requests/{id}', {
        params: { path: { id } },
      });
      if (result.error !== undefined || !result.response.ok) {
        throw new ApiError(result.response.status, result.error);
      }
    },
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: opinionKeys.detail(id) });
      invalidateRequest(queryClient);
    },
  });
}

// ── 기관 회신 ──────────────────────────────────────────────

export function useMyOpinionReply(requestId: number | null, enabled = true) {
  return useQuery({
    queryKey: opinionKeys.myReply(requestId ?? 0),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/opinion-requests/{requestId}/my-reply', {
          params: { path: { requestId: requestId! } },
        }),
      ),
    enabled: requestId != null && enabled,
  });
}

function onReplyChanged(queryClient: QueryClient, requestId: number, reply: OpinionMyReply) {
  queryClient.setQueryData(opinionKeys.myReply(requestId), reply);
  void queryClient.invalidateQueries({ queryKey: ['opinion-requests', 'list'] });
  void queryClient.invalidateQueries({ queryKey: homeKeys.all });
}

export function useSaveOpinionReplyDraft(requestId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: OpinionReplySaveRequest) =>
      unwrap(
        await api.PUT('/api/v1/opinion-requests/{requestId}/my-reply', {
          params: { path: { requestId } },
          body,
        }),
      ),
    onSuccess: (reply) => onReplyChanged(queryClient, requestId, reply),
  });
}

export function useSubmitOpinionReply(requestId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: OpinionReplySaveRequest) =>
      unwrap(
        await api.POST('/api/v1/opinion-requests/{requestId}/my-reply/submission', {
          params: { path: { requestId } },
          body,
        }),
      ),
    onSuccess: (reply) => onReplyChanged(queryClient, requestId, reply),
  });
}

/** 제출한 회신의 수정 요청. 결제원 담당자가 승인하면 다시 고칠 수 있다. */
export function useRequestOpinionReplyRevision(requestId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (reason: string) =>
      unwrap(
        await api.POST('/api/v1/opinion-requests/{requestId}/my-reply/revision-request', {
          params: { path: { requestId } },
          body: { reason },
        }),
      ),
    onSuccess: (reply) => onReplyChanged(queryClient, requestId, reply),
  });
}

/** 수정 요청 승인·반려(결제원 담당자). 반려에는 사유가 있다. */
export function useReviewOpinionReplyRevision(requestId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      replyId,
      approve,
      reason,
    }: {
      replyId: number;
      approve: boolean;
      reason?: string;
    }) => {
      const result = approve
        ? await api.POST(
            '/api/v1/opinion-requests/{requestId}/replies/{replyId}/revision-approval',
            { params: { path: { requestId, replyId } } },
          )
        : await api.POST(
            '/api/v1/opinion-requests/{requestId}/replies/{replyId}/revision-rejection',
            { params: { path: { requestId, replyId } }, body: { reason: reason ?? '' } },
          );
      if (result.error !== undefined || !result.response.ok) {
        throw new ApiError(result.response.status, result.error);
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: opinionKeys.collation(requestId) });
      void queryClient.invalidateQueries({ queryKey: opinionKeys.detail(requestId) });
    },
  });
}

// ── 회신 현황·취합 ─────────────────────────────────────────

export function useOpinionCollation(requestId: number | null, enabled = true) {
  return useQuery({
    queryKey: opinionKeys.collation(requestId ?? 0),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/opinion-requests/{requestId}/collation', {
          params: { path: { requestId: requestId! } },
        }),
      ),
    enabled: requestId != null && enabled,
  });
}

export function useOpinionReminders(requestId: number | null, enabled = true) {
  return useQuery({
    queryKey: opinionKeys.reminders(requestId ?? 0),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/opinion-requests/{requestId}/reminders', {
          params: { path: { requestId: requestId! } },
        }),
      ),
    enabled: requestId != null && enabled,
  });
}

/** 미회신 기관에 리마인드 메일. 기관을 고르지 않으면 미회신 기관 전체다. */
export function useSendOpinionReminder(requestId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (organizationIds?: number[]) =>
      unwrap(
        await api.POST('/api/v1/opinion-requests/{requestId}/reminders', {
          params: { path: { requestId } },
          body: { organizationIds: organizationIds ?? null },
        }),
      ),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: opinionKeys.reminders(requestId) });
    },
  });
}

/**
 * 취합표 엑셀 내려받기. 내부망 전용이라(데모의 망 전환 헤더를 실어야 해서) 링크가 아니라 API 클라이언트로 받아 저장한다.
 * 인터넷망이면 403 INTERNAL_ONLY 로 끝난다.
 */
export function useDownloadOpinionExcel(requestId: number) {
  return useMutation({
    mutationFn: async () => {
      const result = await api.GET('/api/v1/opinion-requests/{requestId}/collation/excel', {
        params: { path: { requestId } },
        parseAs: 'blob',
      });
      if (result.error !== undefined || !result.response.ok || !result.data) {
        throw new ApiError(result.response.status, result.error);
      }
      const blob = result.data as unknown as Blob;
      saveBlob(blob, fileNameOf(result.response) ?? '의견취합.xlsx');
    },
  });
}

/** Content-Disposition 의 filename*=UTF-8''… (한글 이름) 을 읽는다. */
export function fileNameOf(response: Response): string | undefined {
  const header = response.headers.get('Content-Disposition') ?? '';
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(header)?.[1];
  if (encoded) return decodeURIComponent(encoded);
  return /filename="?([^";]+)"?/i.exec(header)?.[1];
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1_000);
}

/** 첨부를 올리거나 지운 뒤 다른 대상(양식 파일 등) 목록도 새로 읽게 할 때. */
export function invalidateOpinionAttachments(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: attachmentKeys.all });
}
