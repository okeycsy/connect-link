import { type QueryClient, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

import { api, ApiError, unwrap } from '../../api/client';
import type { components } from '../../api/schema';
import { isDemoProfile, useServerInfo } from '../../app/hooks';
import { DEFAULT_BUSINESS_SLUG } from '../../app/navigation';

export type CurrentUserResponse = components['schemas']['CurrentUserResponse'];
export type LoginRequest = components['schemas']['LoginRequest'];
export type DemoAccountResponse = components['schemas']['DemoAccountResponse'];
export type EmailVerificationResponse = components['schemas']['EmailVerificationResponse'];
export type EmailVerificationConfirmResponse =
  components['schemas']['EmailVerificationConfirmResponse'];
export type SignUpRequest = components['schemas']['SignUpRequest'];
export type SignUpResponse = components['schemas']['SignUpResponse'];

export const accountKeys = {
  me: ['me'] as const,
  demoAccounts: ['demo-accounts'] as const,
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
