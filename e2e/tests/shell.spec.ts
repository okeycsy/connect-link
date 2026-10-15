import { expect, test } from '@playwright/test';

test.describe('앱 셸', () => {
  test('홈은 레일에 업무 영역 네 개를 보여 주고 기관 목록을 불러온다', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/account-info\/home$/);

    const rail = page.getByRole('navigation');
    await expect(rail.getByRole('link', { name: '계좌통합관리' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    await expect(rail.getByRole('link', { name: '자동이체통합관리' })).toBeVisible();
    await expect(rail.getByRole('link', { name: '대출이동' })).toBeVisible();
    await expect(rail.getByRole('link', { name: 'CMS' })).toBeVisible();

    const cards = page.getByTestId('home-cards');
    await expect(cards.getByText('확인이 필요한 담당자')).toBeVisible();
    await expect(page.getByRole('heading', { name: '안녕하세요, 데모 관리자 님' })).toBeVisible();

    await page.screenshot({ path: 'screenshots/home.png', fullPage: true });
  });

  test('레일을 바꾸면 같은 기능 화면을 다른 업무 영역으로 연다', async ({ page }) => {
    await page.goto('/account-info/contacts');
    await page.getByRole('navigation').getByRole('link', { name: 'CMS' }).click();
    await expect(page).toHaveURL(/\/cms\/contacts$/);
    await expect(page.getByRole('heading', { name: '연락처' })).toBeVisible();
    await expect(page.getByText('CMS의 연락처 화면은')).toBeVisible();
  });

  test('모르는 업무 영역 주소는 기본 업무 홈으로 보낸다', async ({ page }) => {
    await page.goto('/nope/home');
    await expect(page).toHaveURL(/\/account-info\/home$/);
  });

  test('로그인 화면이 뜬다', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: '로그인' })).toBeVisible();
    await expect(page.getByLabel('회사 이메일')).toBeVisible();
    await page.screenshot({ path: 'screenshots/login.png', fullPage: true });
  });

  test('API 문서와 상태 API 가 열려 있다', async ({ request }) => {
    const health = await request.get('/api/v1/health');
    expect(health.ok()).toBeTruthy();
    expect((await health.json()).profile).toBe('demo');

    const docs = await request.get('/v3/api-docs');
    expect(docs.ok()).toBeTruthy();

    // demo 프로필은 로그인 없는 요청을 데모 관리자로 인증한다(운영 프로필은 401).
    const me = await request.get('/api/v1/me');
    expect(me.ok()).toBeTruthy();
    expect((await me.json()).email).toBe('admin@kftc.example.com');
  });
});
