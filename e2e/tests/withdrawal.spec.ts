import { expect, type Page, test } from '@playwright/test';

import {
  applyForGana,
  approveAsHong,
  closeToast,
  csrf,
  mailbox,
  NEW_ACCOUNT_PASSWORD as PASSWORD,
} from './support/account';

// 공통 세션(데모 관리자)은 탈퇴시키지 않는다. 새로 가입·승인한 계정으로 빈 브라우저에서 시작한다.
test.use({ storageState: { cookies: [], origins: [] } });

/** 가입 승인 표의 한 행(신청자 이름으로 찾는다). */
const row = (page: Page, name: string) =>
  page.getByTestId('sign-up-table').getByRole('button', { name: `${name} 가입 신청` });

test.describe('탈퇴', () => {
  test('비밀번호를 다시 넣어 탈퇴하면 모든 세션이 끝나고 같은 이메일로 다시 가입할 수 있다', async ({
    page,
    browser,
    request,
  }) => {
    const email = `leave.${Date.now().toString(36).slice(-6)}@kb.example.com`;
    await applyForGana(request, email, '이탈퇴');
    await approveAsHong(request, email);

    // 다른 브라우저에도 같은 계정으로 로그인해 둔다.
    const elsewhere = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const login = await elsewhere.request.post('/api/v1/auth/login', {
      data: { email, password: PASSWORD },
      headers: await csrf(elsewhere.request),
    });
    expect(login.ok()).toBeTruthy();

    await page.goto('/login');
    await page.getByLabel('회사 이메일').fill(email);
    await page.getByLabel('비밀번호').fill(PASSWORD);
    await page.getByRole('button', { name: '로그인', exact: true }).click();
    await expect(page).toHaveURL(/\/account-info\/home$/);

    await page.getByRole('button', { name: '사용자 메뉴' }).click();
    await page.getByRole('menuitem', { name: '내 정보' }).click();
    const card = page.getByTestId('my-withdraw');
    const open = card.getByRole('button', { name: '탈퇴하기' });
    await expect(open).toBeEnabled();
    await expect(page.getByTestId('withdraw-blockers')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/withdrawal-my-info.png', fullPage: true });

    // 비밀번호가 틀리면 칸 아래에 서버 메시지가 보이고 탈퇴하지 않는다.
    await open.click();
    const dialog = page.getByRole('dialog', { name: '탈퇴' });
    await expect(dialog).toContainText('KB국민은행 이탈퇴 님의 연결고리 계정을 탈퇴합니다.');
    await dialog.getByLabel(/비밀번호/).fill('Wrong123!');
    await dialog.getByRole('button', { name: '탈퇴하기' }).click();
    await expect(dialog.getByText('비밀번호가 맞지 않습니다.')).toBeVisible();
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/withdrawal-wrong-password.png', fullPage: true });

    await dialog.getByLabel(/비밀번호/).fill(PASSWORD);
    await expect(dialog.getByText('비밀번호가 맞지 않습니다.')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/withdrawal-confirm.png', fullPage: true });
    await dialog.getByRole('button', { name: '탈퇴하기' }).click();

    // 로그인 화면으로 가서 탈퇴 완료를 알린다.
    await expect(page).toHaveURL(/\/login\?withdrawn=1$/);
    await expect(page.getByTestId('login-withdrawn')).toContainText('탈퇴가 완료되었습니다.');
    await page.screenshot({ path: 'screenshots/withdrawal-done.png', fullPage: true });

    // 탈퇴 확인 메일이 가고, 다른 브라우저의 세션도 끝났다.
    const mail = (await mailbox(request, email))[0];
    expect(mail.subject).toBe('[연결고리] 탈퇴가 완료되었습니다');
    expect(mail.body).toContain('KB국민은행 이탈퇴 님의 연결고리 탈퇴가 완료되었습니다.');
    expect((await elsewhere.request.get('/api/v1/me')).status()).toBe(401);
    await elsewhere.close();

    // 같은 이메일·비밀번호로는 로그인하지 못한다.
    await page.getByLabel('회사 이메일').fill(email);
    await page.getByLabel('비밀번호').fill(PASSWORD);
    await page.getByRole('button', { name: '로그인', exact: true }).click();
    await expect(page.getByTestId('login-error')).toContainText(
      '이메일 또는 비밀번호가 맞지 않습니다.',
    );
    await expect(page.getByTestId('login-withdrawn')).toHaveCount(0);

    // 같은 회사 이메일로 다시 가입할 수 있다(인증 코드를 새로 받는다).
    const again = await request.post('/api/v1/email-verifications', {
      data: { email },
      headers: await csrf(request),
    });
    expect(again.status()).toBe(201);
  });

  test('관리자가 한 명뿐이면 내 정보에 까닭을 보이고 탈퇴 버튼을 막는다', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: /^데모 관리자 .*로 로그인$/ }).click();
    await expect(page).toHaveURL(/\/account-info\/home$/);
    await page.goto('/account-info/me');
    await expect(page.getByTestId('withdraw-blockers')).toContainText(
      '포털 관리자가 한 명뿐이라 탈퇴할 수 없습니다.',
    );
    await expect(
      page.getByTestId('my-withdraw').getByRole('button', { name: '탈퇴하기' }),
    ).toBeDisabled();
    await page.screenshot({ path: 'screenshots/withdrawal-blocked.png', fullPage: true });
  });

  test('결제원 담당자는 가입 승인 화면에서 맡은 업무의 기관 담당자를 사유와 함께 탈퇴 처리한다', async ({
    page,
    browser,
    request,
  }) => {
    const email = `moved.${Date.now().toString(36).slice(-6)}@kb.example.com`;
    const name = '이동해';
    await applyForGana(request, email, name);
    await approveAsHong(request, email);
    const approvedList = (await (
      await request.get('/api/v1/sign-ups?business=account-info&status=ACTIVE&size=100')
    ).json()) as { content: { id: number; email: string }[] };
    const id = approvedList.content.find((a) => a.email === email)!.id;

    // 그 업무를 맡지 않은 결제원 담당자(박하늘, CMS)는 API 로도 처리하지 못한다.
    const park = await request.post('/api/v1/auth/login', {
      data: { email: 'park.cms@kftc.example.com', password: 'demo1234!' },
      headers: await csrf(request),
    });
    expect(park.ok()).toBeTruthy();
    const denied = await request.post(`/api/v1/accounts/${id}/withdrawal`, {
      data: { reason: '사유' },
      headers: await csrf(request),
    });
    expect(denied.status()).toBe(403);

    // 탈퇴 처리될 사람은 다른 브라우저에 로그인해 있다.
    const target = await browser.newContext({ storageState: { cookies: [], origins: [] } });
    const login = await target.request.post('/api/v1/auth/login', {
      data: { email, password: PASSWORD },
      headers: await csrf(target.request),
    });
    expect(login.ok()).toBeTruthy();

    await page.goto('/login');
    await page.getByRole('button', { name: '홍길동 (결제원 · 현업)로 로그인' }).click();
    await expect(page).toHaveURL(/\/account-info\/home$/);
    await page.goto('/account-info/admin/sign-ups');
    await page
      .getByRole('group', { name: '상태' })
      .getByText(/^승인 \d+$/)
      .click();
    await row(page, name).click();
    const detail = page.getByTestId('sign-up-detail');
    await expect(detail).toContainText(email);
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/withdrawal-staff-drawer.png', fullPage: true });

    await page.getByRole('button', { name: '탈퇴 처리', exact: true }).click();
    const dialog = page.getByRole('dialog', { name: '탈퇴 처리' });
    await expect(dialog).toContainText(`KB국민은행 ${name} 님의 계정을 탈퇴 처리합니다.`);
    const submit = dialog.getByRole('button', { name: '탈퇴 처리' });
    await expect(submit).toBeEnabled();
    const reason = '인사이동으로 계좌통합관리 담당이 끝났다고 KB국민은행에서 알려 왔습니다.';
    await dialog.getByLabel(/탈퇴 처리 사유/).fill(reason);
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/withdrawal-staff-confirm.png', fullPage: true });
    await submit.click();
    await closeToast(page, `${name} 님을 탈퇴 처리했습니다.`);
    await expect(row(page, name)).toHaveCount(0);

    // 탈퇴 처리된 사람에게 사유를 담은 메일이 가고, 그 사람의 세션은 끝났다.
    const mail = (await mailbox(request, email))[0];
    expect(mail.subject).toBe('[연결고리] 계정이 탈퇴 처리되었습니다');
    expect(mail.body).toContain('결제원 담당자 홍길동 님이 탈퇴 처리했습니다');
    expect(mail.body).toContain(`사유: ${reason}`);
    expect((await target.request.get('/api/v1/me')).status()).toBe(401);
    await target.close();
  });
});
