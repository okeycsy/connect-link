import createClient, { type Middleware } from 'openapi-fetch';

import type { paths } from './schema';

/** 세션 쿠키 방식이라 CSRF 토큰(XSRF-TOKEN 쿠키)을 변경 요청 헤더에 실어 보낸다. */
const csrfMiddleware: Middleware = {
  onRequest({ request }) {
    if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method)) {
      const token = readCookie('XSRF-TOKEN');
      if (token) {
        request.headers.set('X-XSRF-TOKEN', token);
      }
    }
    return request;
  },
};

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`))
    ?.split('=')[1];
}

/**
 * 모든 API 호출은 이 클라이언트를 거친다. 경로와 타입은 src/api/schema.d.ts(생성물)에서 온다.
 * 컴포넌트에서 fetch 를 직접 쓰지 않는다.
 */
export const api = createClient<paths>({ baseUrl: '/', credentials: 'same-origin' });
api.use(csrfMiddleware);

/** openapi-fetch 응답에서 데이터만 꺼내고, 오류면 서버의 ErrorResponse 를 던진다. */
export function unwrap<T>(result: { data?: T; error?: unknown; response: Response }): T {
  if (result.error !== undefined || result.data === undefined) {
    throw new ApiError(result.response.status, result.error);
  }
  return result.data;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: unknown,
  ) {
    super(extractDetail(body) ?? `요청에 실패했습니다 (${status})`);
    this.name = 'ApiError';
  }
}

function extractDetail(body: unknown): string | undefined {
  if (body && typeof body === 'object' && 'detail' in body) {
    const detail = (body as { detail?: unknown }).detail;
    return typeof detail === 'string' ? detail : undefined;
  }
  return undefined;
}

/** 서버 검증 오류(400)의 필드별 메시지를 폼 오류 꼴({ field: message })로 바꾼다. 검증 오류가 아니면 빈 객체. */
export function fieldErrorsOf(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError) || !error.body || typeof error.body !== 'object') {
    return {};
  }
  const fieldErrors = (error.body as { fieldErrors?: { field: string; message: string }[] })
    .fieldErrors;
  return Object.fromEntries((fieldErrors ?? []).map((f) => [f.field, f.message]));
}
