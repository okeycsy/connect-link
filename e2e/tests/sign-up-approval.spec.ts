import { type APIRequestContext, expect, type Page, test } from '@playwright/test';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 결제원 담당자(홍길동)로 승인한다.
test.use({ storageState: { cookies: [], origins: [] } });

const PASSWORD = 'Welcome1!';

/** 지금 쿠키의 CSRF 토큰. 로그인하면 서버가 토큰을 바꾸므로 요청마다 다시 읽는다. */
async function csrf(request: APIRequestContext): Promise<Record<string, string>> {
  let token = (await request.storageState()).cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
  if (!token) {
    await request.get('/api/v1/health');
    token = (await request.storageState()).cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
  }
  return { 'X-XSRF-TOKEN': token! };
}

/** 데모 받은 메일함(발송함)에서 이 사람에게 간 최근 메일. */
async function mailbox(request: APIRequestContext, email: string) {
  const response = await request.get(`/api/v1/demo/mailbox?email=${encodeURIComponent(email)}`);
  expect(response.ok()).toBeTruthy();
  return (await response.json()) as { subject: string; body: string }[];
}

/** 화면을 거치지 않고 가입 API 로 가나은행 계좌통합관리 담당자의 가입을 신청한다(인증 코드는 데모 메일함에서 읽는다). */
async function applyForGana(request: APIRequestContext, email: string, name: string) {
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
      password: PASSWORD,
    },
    headers: await csrf(request),
  });
  expect(signUp.ok(), await signUp.text()).toBeTruthy();
}

/** 처리 결과 알림(토스트)을 확인하고 닫는다. 다음 캡처를 가리지 않게 한다. */
async function closeToast(page: Page, text: string) {
  const toast = page.getByRole('alert').filter({ hasText: text });
  await expect(toast).toBeVisible();
  await toast.getByRole('button').click();
  await expect(toast).toHaveCount(0);
}

/** 표의 한 행(신청자 이름으로 찾는다). */
const row = (page: Page, name: string) =>
  page.getByTestId('sign-up-table').getByRole('button', { name: `${name} 가입 신청` });

test.describe('관리 > 가입 승인', () => {
  test('결제원 담당자가 맡은 업무의 가입 신청을 승인하고 반려하면 메일이 간다', async ({
    page,
    request,
  }) => {
    const id = Date.now().toString(36).slice(-6);
    const approvee = { email: `approve.${id}@gana.example.com`, name: '이승인' };
    const rejectee = { email: `reject.${id}@gana.example.com`, name: '박반려' };
    await applyForGana(request, approvee.email, approvee.name);
    await applyForGana(request, rejectee.email, rejectee.name);

    // 결제원 담당자는 관리 메뉴가 보이고, 홈에 가입 승인 대기 카드가 있다.
    await page.goto('/login');
    await page.getByRole('button', { name: '홍길동 (결제원 · 현업)로 로그인' }).click();
    await expect(page).toHaveURL(/\/account-info\/home$/);
    const card = page.getByTestId('home-card-SIGNUP_APPROVAL');
    await expect(card.getByText('가입 승인 대기')).toBeVisible();
    await expect(card.getByText('가나은행 · 박반려')).toBeVisible();
    await page.screenshot({ path: 'screenshots/sign-up-approval-home.png', fullPage: true });

    await page.getByRole('navigation').getByRole('link', { name: '관리', exact: true }).click();
    await expect(page).toHaveURL(/\/account-info\/admin$/);
    const menu = page.getByTestId('admin-menu');
    await expect(menu.getByRole('link', { name: /가입 승인/ })).toContainText(/대기 \d+건/);
    // 기준정보는 관리자 전용이라 결제원 담당자에게는 보이지 않는다.
    await expect(menu.getByText('기관·업무 기준정보')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/sign-up-approval-hub.png', fullPage: true });

    await menu.getByRole('link', { name: /가입 승인/ }).click();
    await expect(page).toHaveURL(/\/account-info\/admin\/sign-ups$/);
    await expect(page.getByRole('heading', { name: '가입 승인' })).toBeVisible();
    await expect(row(page, '오다라')).toBeVisible();
    await expect(row(page, '한가나')).toBeVisible();
    await expect(row(page, approvee.name)).toBeVisible();
    await expect(row(page, '오다라')).toContainText('도메인 다름');
    await page.screenshot({ path: 'screenshots/sign-up-approval-list.png', fullPage: true });

    // 도메인이 다른 신청은 상세 패널이 먼저 알려 준다.
    await row(page, '오다라').click();
    const detail = page.getByTestId('sign-up-detail');
    await expect(detail).toContainText('등록된 도메인(dara.example.com)과 다릅니다');
    await expect(detail).toContainText('자동이체 전문 개발');
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/sign-up-approval-detail.png', fullPage: true });
    await page.keyboard.press('Escape');
    await expect(detail).toHaveCount(0);

    // 승인: 확인 창을 거친다.
    await row(page, approvee.name).click();
    await page.getByRole('button', { name: '승인', exact: true }).click();
    const approveDialog = page.getByRole('dialog', { name: '가입 승인' });
    await expect(approveDialog).toContainText(`가나은행 ${approvee.name} 님의 가입을 승인합니다.`);
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/sign-up-approval-approve.png', fullPage: true });
    await approveDialog.getByRole('button', { name: '승인하기' }).click();
    await closeToast(page, `${approvee.name} 님의 가입을 승인했습니다.`);
    await expect(row(page, approvee.name)).toHaveCount(0);

    // 반려: 사유가 없으면 서버 메시지를 칸 아래에 보인다.
    await row(page, rejectee.name).click();
    await page.getByRole('button', { name: '반려', exact: true }).click();
    const rejectDialog = page.getByRole('dialog', { name: '가입 반려' });
    await rejectDialog.getByRole('button', { name: '반려하기' }).click();
    await expect(rejectDialog.getByText('반려 사유를 적어 주세요.')).toBeVisible();
    const reason = '가나은행 계좌통합관리 담당자 명단에서 확인되지 않습니다. 부서장 확인 뒤 다시 신청해 주세요.';
    await rejectDialog.getByLabel(/반려 사유/).fill(reason);
    await expect(rejectDialog.getByText('반려 사유를 적어 주세요.')).toHaveCount(0);
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/sign-up-approval-reject.png', fullPage: true });
    await rejectDialog.getByRole('button', { name: '반려하기' }).click();
    await closeToast(page, `${rejectee.name} 님의 가입 신청을 반려했습니다.`);
    await expect(row(page, rejectee.name)).toHaveCount(0);

    // 반려 칩: 처리한 사람과 날짜, 패널에 반려 사유.
    await page.getByRole('group', { name: '상태' }).getByText(/^반려/).click();
    await expect(row(page, rejectee.name)).toContainText('홍길동');
    await expect(row(page, '서사아')).toBeVisible();
    await row(page, rejectee.name).click();
    await expect(page.getByTestId('sign-up-detail')).toContainText(reason);
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/sign-up-approval-rejected.png', fullPage: true });

    // 메일: 승인 메일과 사유가 담긴 반려 메일이 발송함에 남는다.
    expect((await mailbox(request, approvee.email))[0].subject).toContain('가입이 승인되었습니다');
    const rejection = (await mailbox(request, rejectee.email))[0];
    expect(rejection.subject).toBe('[연결고리] 가입 신청이 반려되었습니다');
    expect(rejection.body).toContain(`반려 사유: ${reason}`);

    // 승인된 사람은 바로 로그인한다.
    const login = await request.post('/api/v1/auth/login', {
      data: { email: approvee.email, password: PASSWORD },
      headers: await csrf(request),
    });
    expect(login.ok()).toBeTruthy();
    expect((await login.json()).name).toBe(approvee.name);
    // 반려된 이메일은 '이미 가입' 으로 막히지 않는다(방금 코드를 받아 재발송 대기에만 걸릴 수 있다). 승인된 이메일은 막힌다.
    const again = await request.post('/api/v1/email-verifications', {
      data: { email: rejectee.email },
      headers: await csrf(request),
    });
    expect(again.ok() || (await again.json()).code === 'VERIFICATION_RESEND_TOO_SOON').toBe(true);
    const approvedAgain = await request.post('/api/v1/email-verifications', {
      data: { email: approvee.email },
      headers: await csrf(request),
    });
    expect(approvedAgain.status()).toBe(409);
    expect((await approvedAgain.json()).code).toBe('EMAIL_ALREADY_REGISTERED');

    // 결제원 담당자도 관리자 전용 항목(기준정보)은 주소로 들어와도 막는다.
    await page.goto('/account-info/admin/organizations');
    await expect(page.getByTestId('state-forbidden')).toContainText('관리자만 볼 수 있습니다');
  });

  test('참가기관 담당자는 관리 메뉴가 없고 가입 승인 API 도 막힌다', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: '김가나 (참가기관 · 현업)로 로그인' }).click();
    await expect(page).toHaveURL(/\/account-info\/home$/);
    await expect(page.getByTestId('home-card-SIGNUP_APPROVAL')).toHaveCount(0);
    await expect(
      page.getByRole('navigation').getByRole('link', { name: '관리', exact: true }),
    ).toHaveCount(0);
    await page.goto('/account-info/admin/sign-ups');
    await expect(page.getByTestId('state-forbidden')).toBeVisible();
    expect((await page.request.get('/api/v1/sign-ups?business=account-info')).status()).toBe(403);
  });
});
