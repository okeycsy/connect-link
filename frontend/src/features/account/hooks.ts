import {
  keepPreviousData,
  type QueryClient,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

import { api, ApiError, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import { isDemoProfile, useServerInfo } from '../../app/hooks';
import { DEFAULT_BUSINESS_SLUG } from '../../app/navigation';
import { homeKeys } from '../home/hooks';
import { notificationKeys } from '../notification/hooks';

export type CurrentUserResponse = components['schemas']['CurrentUserResponse'];
export type LoginRequest = components['schemas']['LoginRequest'];
export type DemoAccountResponse = components['schemas']['DemoAccountResponse'];
export type EmailVerificationResponse = components['schemas']['EmailVerificationResponse'];
export type EmailVerificationConfirmResponse =
  components['schemas']['EmailVerificationConfirmResponse'];
export type SignUpRequest = components['schemas']['SignUpRequest'];
export type SignUpResponse = components['schemas']['SignUpResponse'];
export type SignUpApplicationResponse = components['schemas']['SignUpApplicationResponse'];
export type SignUpApplicationCountsResponse =
  components['schemas']['SignUpApplicationCountsResponse'];
export type WithdrawalCheckResponse = components['schemas']['WithdrawalCheckResponse'];
/** 가입 승인 화면의 상태 칩. 승인은 사용 중(ACTIVE) 계정이다. */
export type SignUpReviewStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'REJECTED';

export interface SignUpApplicationParams {
  /** 업무 영역 slug. */
  business: string;
  status: SignUpReviewStatus;
  page?: number;
  size?: number;
}

export const accountKeys = {
  me: ['me'] as const,
  demoAccounts: ['demo-accounts'] as const,
  supportInfo: ['support-info'] as const,
  withdrawalCheck: ['withdrawal-check'] as const,
};

export const signUpKeys = {
  all: ['sign-ups'] as const,
  list: (params: SignUpApplicationParams) => ['sign-ups', 'list', params] as const,
  counts: (business: string) => ['sign-ups', 'counts', business] as const,
  detail: (id: number) => ['sign-ups', 'detail', id] as const,
  withdrawalCheck: (accountId: number) => ['sign-ups', 'withdrawal-check', accountId] as const,
};

/** 로그인하지 않았거나 세션이 끝났다는 서버 응답(401)인지. */
export function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
}

/** 로그인 사용자. 로그인 전이면 401 로 끝나고 다시 묻지 않는다(셸이 로그인 화면으로 보낸다). */
export function useMe() {
  return useQuery({
    queryKey: accountKeys.me,
    queryFn: async () => unwrap(await api.GET('/api/v1/me')),
    staleTime: 5 * 60_000,
    retry: (failureCount, error) => !isUnauthorized(error) && failureCount < 1,
  });
}

/** 사용자가 볼 수 있는 업무 영역인지. 관리자는 전 업무, 그 밖에는 담당 업무만이다(서버도 같은 규칙으로 막는다). */
export function canViewBusiness(me: CurrentUserResponse, slug: string | undefined): boolean {
  return me.admin || me.businesses.some((b) => b.slug === slug);
}

/**
 * 관리 메뉴를 쓸 수 있는지. 관리자와 결제원 담당자다. 결제원 담당자는 맡은 업무의 가입 승인만 하고, 기준정보 같은 관리자 항목은
 * 관리자에게만 보인다(서버도 같은 규칙으로 막는다).
 */
export function canUseAdminMenu(me: CurrentUserResponse): boolean {
  return me.admin || me.organizationType === 'KFTC';
}

/** 로그인 직후와 볼 수 없는 업무 주소에서 보낼 업무 영역. 담당 업무 중 첫 번째(업무 순서)이고 관리자는 기본 업무다. */
export function homeBusinessSlugOf(me: CurrentUserResponse): string {
  if (me.admin || me.businesses.length === 0) return DEFAULT_BUSINESS_SLUG;
  return me.businesses[0].slug;
}

/** 로그인 뒤 돌아갈 주소. 이 앱 안의 경로만 받는다(다른 사이트로 보내는 주소는 버린다). */
export function safeNextPath(next: string | null): string | undefined {
  if (
    !next ||
    !next.startsWith('/') ||
    next.startsWith('//') ||
    next.startsWith('/\\') ||
    next.startsWith('/login')
  ) {
    return undefined;
  }
  return next;
}

/**
 * 로그인. 성공하면 이전 사용자의 화면 데이터를 모두 버리고 새 사용자 정보로 채운다(계정 전환도 이 길이다).
 * 실패는 서버 ErrorResponse 를 담은 ApiError 다(401 INVALID_CREDENTIALS, 403 ACCOUNT_PENDING_APPROVAL 등).
 */
export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: LoginRequest) =>
      unwrap(await api.POST('/api/v1/auth/login', { body })),
    onSuccess: (me) => {
      clearUserData(queryClient);
      queryClient.setQueryData(accountKeys.me, me);
    },
  });
}

/**
 * 로그아웃. 세션을 끝내고 로그인 화면으로 간 뒤 화면 데이터를 모두 버린다. 서버 호출이 실패해도(이미 세션이 끝난 경우 등) 같은 길로
 * 나간다.
 */
export function useLogout() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  return useMutation({
    mutationFn: async () => {
      const result = await api.POST('/api/v1/auth/logout');
      if (!result.response.ok) throw new ApiError(result.response.status, result.error);
    },
    onSettled: () => {
      navigate('/login', { replace: true });
      clearUserData(queryClient);
    },
  });
}

/** 내 정보 > 탈퇴: 지금 탈퇴할 수 있는지와, 막혀 있으면 그 까닭(할 일과 그 화면 주소). */
export function useWithdrawalCheck() {
  return useQuery({
    queryKey: accountKeys.withdrawalCheck,
    queryFn: async () => unwrap(await api.GET('/api/v1/me/withdrawal')),
  });
}

/**
 * 탈퇴. 비밀번호가 틀리면 400(password 칸 오류), 그사이 막는 까닭이 생겼으면 409 WITHDRAWAL_BLOCKED 이고 탈퇴 가능 여부를 다시 읽는다.
 * 성공하면 서버가 세션을 끝내므로 로그인 화면(탈퇴 완료 안내)으로 가고 화면 데이터를 모두 버린다.
 */
export function useWithdraw() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  return useMutation({
    mutationFn: async (password: string) => {
      const result = await api.POST('/api/v1/me/withdrawal', { body: { password } });
      if (!result.response.ok) throw new ApiError(result.response.status, result.error);
    },
    onSuccess: () => {
      navigate('/login?withdrawn=1', { replace: true });
      clearUserData(queryClient);
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: accountKeys.withdrawalCheck });
      }
    },
  });
}

/** 사용자가 바뀌면 이전 사용자의 화면 데이터를 남기지 않는다. 서버 프로필 정보만 사용자와 무관해서 둔다. */
function clearUserData(queryClient: QueryClient) {
  queryClient.removeQueries({ predicate: (q) => q.queryKey[0] !== 'server-info' });
}

/** 데모 도구: 역할별 데모 계정과 공통 비밀번호. demo 프로필에서만 부른다(운영에는 API 가 없다). */
export function useDemoAccounts() {
  const { data: serverInfo } = useServerInfo();
  return useQuery({
    queryKey: accountKeys.demoAccounts,
    queryFn: async () => unwrap(await api.GET('/api/v1/demo/accounts')),
    enabled: isDemoProfile(serverInfo?.profile),
    staleTime: 5 * 60_000,
  });
}

/** 로그인 화면의 문의 안내(문의 메일 주소). 설정하지 않은 서버는 inquiryEmail 이 비어 문의하기 칸을 뺀다. */
export function useSupportInfo() {
  return useQuery({
    queryKey: accountKeys.supportInfo,
    queryFn: async () => unwrap(await api.GET('/api/v1/support-info')),
    staleTime: 30 * 60_000,
  });
}

/** 가입 ①: 회사 이메일로 인증 코드 받기. 실패는 409 EMAIL_ALREADY_REGISTERED, 429 VERIFICATION_RESEND_TOO_SOON 등. */
export function useRequestVerification() {
  return useMutation({
    mutationFn: async (email: string) =>
      unwrap(await api.POST('/api/v1/email-verifications', { body: { email } })),
  });
}

/** 가입 ①: 받은 코드 확인. 맞으면 가입 신청에 쓸 인증 토큰과 도메인이 같은 기관을 준다. */
export function useConfirmVerification() {
  return useMutation({
    mutationFn: async (body: { email: string; code: string }) =>
      unwrap(await api.POST('/api/v1/email-verifications/confirm', { body })),
  });
}

/** 가입 신청. 성공하면 승인 대기 계정이 생긴다(승인 전에는 로그인할 수 없다). */
export function useSignUp() {
  return useMutation({
    mutationFn: async (body: SignUpRequest) => unwrap(await api.POST('/api/v1/sign-ups', { body })),
  });
}

/** 관리 > 가입 승인: 한 업무 영역의 신청 목록. 칩·쪽을 바꿔도 이전 결과를 잠시 보여 줘 표가 깜빡이지 않게 한다. */
export function useSignUpApplications(params: SignUpApplicationParams) {
  return useQuery({
    queryKey: signUpKeys.list(params),
    queryFn: async () => unwrap(await api.GET('/api/v1/sign-ups', { params: { query: params } })),
    placeholderData: keepPreviousData,
  });
}

/** 상태 칩에 붙는 건수(승인 대기, 승인, 반려). */
export function useSignUpCounts(business: string | undefined) {
  return useQuery({
    queryKey: signUpKeys.counts(business ?? ''),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/sign-ups/counts', { params: { query: { business: business! } } }),
      ),
    enabled: !!business,
  });
}

export function useSignUpApplication(id: number | null) {
  return useQuery({
    queryKey: signUpKeys.detail(id ?? 0),
    queryFn: async () =>
      unwrap(await api.GET('/api/v1/sign-ups/{id}', { params: { path: { id: id! } } })),
    enabled: id != null,
  });
}

/** 승인·반려 뒤에는 신청 목록·건수, 홈 가입 승인 카드, 알림 센터를 다시 읽는다. */
function invalidateAfterReview(queryClient: QueryClient) {
  queryClient.invalidateQueries({ queryKey: signUpKeys.all });
  queryClient.invalidateQueries({ queryKey: homeKeys.all });
  queryClient.invalidateQueries({ queryKey: notificationKeys.all });
}

/** 승인. 신청자에게 알림 센터 알림과 승인 메일이 간다. 이미 처리한 신청이면 409 SIGN_UP_ALREADY_REVIEWED. */
export function useApproveSignUp() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>
      unwrap(await api.POST('/api/v1/sign-ups/{id}/approval', { params: { path: { id } } })),
    onSuccess: () => invalidateAfterReview(queryClient),
  });
}

/** 탈퇴 처리: 이 계정을 지금 탈퇴 처리할 수 있는지와 막는 까닭. 확인 창을 열 때만 읽는다. */
export function useAccountWithdrawalCheck(accountId: number, enabled: boolean) {
  return useQuery({
    queryKey: signUpKeys.withdrawalCheck(accountId),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/accounts/{id}/withdrawal', { params: { path: { id: accountId } } }),
      ),
    enabled,
  });
}

/**
 * 결제원 담당자의 탈퇴 처리. 사유가 탈퇴 처리된 담당자에게 가는 메일에 그대로 들어간다. 사유가 비면 400 과 reason 필드 오류, 막는 까닭이 생겼으면
 * 409 WITHDRAWAL_BLOCKED.
 */
export function useWithdrawAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) => {
      const result = await api.POST('/api/v1/accounts/{id}/withdrawal', {
        params: { path: { id } },
        body: { reason },
      });
      if (!result.response.ok) throw new ApiError(result.response.status, result.error);
    },
    onSuccess: () => invalidateAfterReview(queryClient),
  });
}

/** 반려. 사유가 반려 메일에 그대로 들어간다. 사유가 비면 400 과 reason 필드 오류. */
export function useRejectSignUp() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, reason }: { id: number; reason: string }) =>
      unwrap(
        await api.POST('/api/v1/sign-ups/{id}/rejection', {
          params: { path: { id } },
          body: { reason },
        }),
      ),
    onSuccess: () => invalidateAfterReview(queryClient),
  });
}
