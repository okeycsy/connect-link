import { expect, type Page, test } from '@playwright/test';

import { skipContactReconfirm } from './support/account';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다.
test.use({ storageState: { cookies: [], origins: [] } });

const PASSWORD = 'demo1234!';

async function loginWithForm(page: Page, email: string, password: string) {
  await page.getByLabel('회사 이메일').fill(email);
  await page.getByLabel('비밀번호').fill(password);
  await page.getByRole('button', { name: '로그인', exact: true }).click();
}

const rail = (page: Page) => page.getByRole('navigation');
const BUSINESSES = ['계좌통합관리', '자동이체통합관리', '대출이동', 'CMS'];

async function railLabels(page: Page): Promise<string[]> {
  const labels: string[] = [];
  for (const name of BUSINESSES) {
    if ((await rail(page).getByRole('link', { name, exact: true }).count()) > 0) labels.push(name);
  }
  return labels;
}

test.describe('로그인', () => {
  test.beforeEach(async ({ page }) => skipContactReconfirm(page));

  test('로그인하지 않으면 로그인 화면으로 보내고, 로그인하면 원래 주소로 돌아간다', async ({
    page,
  }) => {
    await page.goto('/cms/notifications');
    await expect(page).toHaveURL(/\/login\?next=%2Fcms%2Fnotifications$/);
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();
    const demo = page.getByTestId('demo-accounts');
    await expect(demo.getByText('데모 관리자')).toBeVisible();
    await expect(demo.getByText('김민준')).toBeVisible();
    // 승인 대기 계정은 데모 계정 목록에 없다.
    await expect(demo.getByText('정다은')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/login.png', fullPage: true });

    await loginWithForm(page, 'admin@kftc.example.com', PASSWORD);
    await expect(page).toHaveURL(/\/cms\/notifications$/);
    await expect(page.getByRole('heading', { name: '알림 센터' })).toBeVisible();
  });

  test('틀린 비밀번호와 승인 전 계정은 서버 메시지를 보여 준다', async ({ page }) => {
    await page.goto('/login');
    await loginWithForm(page, 'kim.minjun@kb.example.com', 'wrong-password');
    await expect(page.getByTestId('login-error')).toHaveText(
      '이메일 또는 비밀번호가 맞지 않습니다.',
    );

    await loginWithForm(page, 'jung.daeun@woori.example.com', PASSWORD);
    await expect(page.getByTestId('login-error')).toContainText('가입 승인 전입니다.');
    await expect(page).toHaveURL(/\/login$/);
    await page.screenshot({ path: 'screenshots/login-pending.png', fullPage: true });
  });

  test('참가기관 담당자는 자기 업무만 보고 관리 메뉴가 없다', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: '김민준 (참가기관 · 현업)로 로그인' }).click();
    await expect(page).toHaveURL(/\/account-info\/home$/);
    await expect(page.getByRole('heading', { name: '안녕하세요, 김민준 님' })).toBeVisible();
    expect(await railLabels(page)).toEqual(['계좌통합관리']);
    await expect(rail(page).getByRole('link', { name: '관리', exact: true })).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/login-member-home.png', fullPage: true });

    // 담당이 아닌 업무와 관리 주소는 화면도 서버도 막는다.
    await page.goto('/cms/home');
    await expect(page).toHaveURL(/\/account-info\/home$/);
    await page.goto('/account-info/admin');
    await expect(page.getByTestId('state-forbidden')).toBeVisible();
    expect((await page.request.get('/api/v1/home?business=cms')).status()).toBe(403);
    expect(
      (await page.request.get('/api/v1/organizations/search?business=account-info')).status(),
    ).toBe(403);
  });

  test('데모 도구에서 계정을 바꾸고, 로그아웃하면 다시 로그인 화면이다', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: '김민준 (참가기관 · 현업)로 로그인' }).click();
    await expect(page).toHaveURL(/\/account-info\/home$/);

    await page.getByRole('button', { name: '데모 도구' }).click();
    await page.getByRole('menuitem', { name: '계정 전환' }).click();
    const dialog = page.getByRole('dialog', { name: '계정 전환' });
    await expect(dialog.getByText('지금 계정')).toBeVisible();
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/account-switch.png' });

    await dialog.getByRole('button', { name: /^이준호 / }).click();
    await expect(page).toHaveURL(/\/auto-transfer\/home$/);
    await expect(page.getByRole('heading', { name: '안녕하세요, 이준호 님' })).toBeVisible();
    expect(await railLabels(page)).toEqual(['자동이체통합관리', 'CMS']);

    await page.getByRole('button', { name: '사용자 메뉴' }).click();
    await expect(page.getByTestId('user-name')).toHaveText('이준호');
    await page.getByRole('menuitem', { name: '로그아웃' }).click();
    await expect(page).toHaveURL(/\/login$/);

    await page.goto('/auto-transfer/home');
    await expect(page).toHaveURL(/\/login\?next=%2Fauto-transfer%2Fhome$/);
    expect((await page.request.get('/api/v1/me')).status()).toBe(401);
  });

  test('로그인 전에는 보호된 API 가 401 이고 데모 계정 목록만 열려 있다', async ({ request }) => {
    expect((await request.get('/api/v1/me')).status()).toBe(401);
    const demo = await request.get('/api/v1/demo/accounts');
    expect(demo.ok()).toBeTruthy();
    const body = await demo.json();
    expect(body.accounts[0].email).toBe('admin@kftc.example.com');
    expect(body.accounts.map((a: { email: string }) => a.email)).not.toContain(
      'jung.daeun@woori.example.com',
    );
  });
});

test.describe('로그인 화면 안내', () => {
  test('기능 소개, 이용 가이드, 문의 메일을 보이고 가이드는 로그인 없이 열린다', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto('/login');
    const features = page.getByRole('region', { name: '연결이 주요 기능' });
    for (const title of [
      '담당자 연락처',
      '의견 요청과 회신',
      '기관 테스트',
      '일시중단 공유',
      '소통 채널',
      '공지·자료실',
    ]) {
      await expect(features.getByText(title, { exact: true })).toBeVisible();
    }
    await expect(page.getByRole('link', { name: '문의 메일 보내기' })).toHaveAttribute(
      'href',
      'mailto:help@connect-link.example.com',
    );

    await page.getByRole('link', { name: '가이드 보기' }).click();
    await expect(page).toHaveURL(/\/guide$/);
    await page.getByRole('link', { name: /의견 요청·회신 작성 가이드/ }).click();
    await expect(page).toHaveURL(/\/guide\/opinions$/);
    await expect(page.getByRole('heading', { name: '이용 가이드' })).toBeVisible();
    const guide = page.getByTestId('user-guide');
    await expect(guide.getByRole('heading', { name: '회신 쓰기' })).toBeVisible();
    await guide
      .getByRole('link', { name: /기관 테스트 결과 등록 방법/ })
      .last()
      .click();
    await expect(page).toHaveURL(/\/guide\/tests$/);
    await expect(guide.getByRole('heading', { name: '결과 등록하기' })).toBeVisible();
  });

  test('휴대폰 폭에서는 기능 소개를 빼고 가이드·문의는 남긴다', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();
    await expect(page.getByRole('region', { name: '연결이 주요 기능' })).toBeHidden();
    await expect(page.getByRole('link', { name: '가이드 보기' })).toBeVisible();
    await expect(page.getByRole('link', { name: '문의 메일 보내기' })).toBeVisible();
  });
});
