import { type APIRequestContext, expect, type Page } from '@playwright/test';

/** 시나리오에서 새로 가입시키는 계정의 비밀번호. */
export const NEW_ACCOUNT_PASSWORD = 'Welcome1!';

/** 지금 쿠키의 CSRF 토큰. 로그인하면 서버가 토큰을 바꾸므로 요청마다 다시 읽는다. */
export async function csrf(request: APIRequestContext): Promise<Record<string, string>> {
  let token = (await request.storageState()).cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
  if (!token) {
    await request.get('/api/v1/health');
    token = (await request.storageState()).cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
  }
  return { 'X-XSRF-TOKEN': token! };
}

/** 데모 받은 메일함(발송함)에서 이 사람에게 간 최근 메일. */
export async function mailbox(request: APIRequestContext, email: string) {
  const response = await request.get(`/api/v1/demo/mailbox?email=${encodeURIComponent(email)}`);
  expect(response.ok()).toBeTruthy();
  return (await response.json()) as { subject: string; body: string }[];
}

/** 화면을 거치지 않고 가입 API 로 가나은행 계좌통합관리 담당자의 가입을 신청한다(인증 코드는 데모 메일함에서 읽는다). */
export async function applyForGana(request: APIRequestContext, email: string, name: string) {
  const sent = await request.post('/api/v1/email-verifications', {
    data: { email },
    headers: await csrf(request),
  });
  expect(sent.ok()).toBeTruthy();
  const code = /인증 코드: (\d{6})/.exec((await mailbox(request, email))[0].body)?.[1];
  expect(code).toBeTruthy();
  const confirmed = await request.post('/api/v1/email-verifications/confirm', {
    data: { email, code },
    headers: await csrf(request),
  });
  expect(confirmed.ok()).toBeTruthy();
  const { verificationToken } = await confirmed.json();
  const organizations = (await (await request.get('/api/v1/organizations')).json()) as {
    id: number;
    code: string;
  }[];
  const gana = organizations.find((o) => o.code === 'GANA')!;

  const signUp = await request.post('/api/v1/sign-ups', {
    data: {
      verificationToken,
      email,
      organizationId: gana.id,
      businesses: [{ business: 'ACCOUNT_INFO', subBusiness: '계좌정보 통합조회' }],
      jobRole: 'BUSINESS',
      itRoles: [],
      name,
      department: '디지털금융부',
      team: '계좌서비스팀',
      jobTitle: '대리',
      extensionPhone: '1234',
      mobilePhone: null,
      password: NEW_ACCOUNT_PASSWORD,
    },
    headers: await csrf(request),
  });
  expect(signUp.ok(), await signUp.text()).toBeTruthy();
}

/** 계좌통합관리 결제원 담당자(홍길동)로 로그인해 이 이메일의 가입 신청을 승인한다. request 의 세션은 홍길동으로 바뀐다. */
export async function approveAsHong(request: APIRequestContext, email: string) {
  const login = await request.post('/api/v1/auth/login', {
    data: { email: 'hong.gildong@kftc.example.com', password: 'demo1234!' },
    headers: await csrf(request),
  });
  expect(login.ok()).toBeTruthy();
  const pending = (await (
    await request.get('/api/v1/sign-ups?business=account-info&size=100')
  ).json()) as { content: { id: number; email: string }[] };
  const application = pending.content.find((a) => a.email === email)!;
  const approved = await request.post(`/api/v1/sign-ups/${application.id}/approval`, {
    headers: await csrf(request),
  });
  expect(approved.ok()).toBeTruthy();
}

/** 처리 결과 알림(토스트)을 확인하고 닫는다. 다음 캡처를 가리지 않게 한다. */
export async function closeToast(page: Page, text: string) {
  const toast = page.getByRole('alert').filter({ hasText: text });
  await expect(toast).toBeVisible();
  await toast.getByRole('button').click();
  await expect(toast).toHaveCount(0);
}
