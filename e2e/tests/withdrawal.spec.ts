import { expect, test } from '@playwright/test';

import {
  applyForGana,
  approveAsHong,
  csrf,
  mailbox,
  NEW_ACCOUNT_PASSWORD as PASSWORD,
} from './support/account';

// 공통 세션(데모 관리자)은 탈퇴시키지 않는다. 새로 가입·승인한 계정으로 빈 브라우저에서 시작한다.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('내 정보 > 탈퇴', () => {
  test('비밀번호를 다시 넣어 탈퇴하면 모든 세션이 끝나고 같은 이메일로 다시 가입할 수 있다', async ({
    page,
    browser,
    request,
  }) => {
    const email = `leave.${Date.now().toString(36).slice(-6)}@gana.example.com`;
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
    await expect(dialog).toContainText('가나은행 이탈퇴 님의 연결고리 계정을 탈퇴합니다.');
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
    expect(mail.body).toContain('가나은행 이탈퇴 님의 연결고리 탈퇴가 완료되었습니다.');
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
});
