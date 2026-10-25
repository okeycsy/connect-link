import { expect, type Page, test } from '@playwright/test';

import { skipContactReconfirm } from './support/account';

/** 홈이 세로 스크롤 없이 화면(기본 1280×800) 안에 들어오는지 본다. */
async function expectNoScroll(page: Page) {
  const { scroll, viewport } = await page.evaluate(() => ({
    scroll: document.documentElement.scrollHeight,
    viewport: window.innerHeight,
  }));
  expect(scroll).toBeLessThanOrEqual(viewport);
}

test.describe('홈 대시보드', () => {
  test('건수 요약 없이 할 일이 있는 카드만 보이고, 일시중단은 오늘부터 다가오는 목록이다', async ({
    page,
  }) => {
    await page.goto('/account-info/home');
    await expect(page.getByRole('heading', { name: '안녕하세요, 데모 관리자 님' })).toBeVisible();

    const cards = page.getByTestId('home-cards');
    for (const key of ['SIGNUP_APPROVAL', 'OPINION_REPLIES', 'TEST_PROGRESS', 'SUSPENSIONS']) {
      await expect(cards.getByTestId(`home-card-${key}`)).toBeVisible();
    }
    // 확인이 필요한 담당자는 홈에 두지 않는다(로그인 뒤 확인 창이 맡는다).
    await expect(cards.getByTestId('home-card-CONTACT_CONFIRM')).toHaveCount(0);
    // 건수 요약 타일은 없다.
    await expect(page.getByText('진행 중 기관 테스트', { exact: true })).toHaveCount(1);
    // 윗줄 왼쪽은 가입 승인 대기, 오른쪽 큰 칸은 다가오는 일시중단(요일 칸 없음, 날짜별 묶음)이다. SHY 요청(2026-10-24).
    // 최근 알림은 홈에 두지 않고 상단 바의 알림 벨에서 본다.
    const upcoming = cards.getByTestId('suspension-upcoming');
    await expect(upcoming).toBeVisible();
    await expect(upcoming.getByText(/^오늘 · /)).toBeVisible();
    // 기관명 옆에 원형 로고(파일이 있는 기관만)를 붙인다.
    await expect(upcoming.locator('img').first()).toBeVisible();
    await expect(cards.getByText('최근 소식')).toHaveCount(0);
    const suspensionBox = (await cards.getByTestId('home-card-SUSPENSIONS').boundingBox())!;
    const signUpBox = (await cards.getByTestId('home-card-SIGNUP_APPROVAL').boundingBox())!;
    expect(signUpBox.x).toBeLessThan(suspensionBox.x);
    // 다가오는 일시중단은 3건까지라 윗줄이 자리를 덜 차지한다. SHY 요청(2026-10-25).
    await expect(upcoming.getByRole('link')).toHaveCount(3);
    expect(suspensionBox.height).toBeLessThan(480);
    // 윗줄 두 카드는 높이를 맞춘다.
    expect(Math.abs(suspensionBox.height - signUpBox.height)).toBeLessThan(2);
    // 임박·초과 글자 배지는 없다.
    await expect(cards.getByText(/^(임박|초과)$/)).toHaveCount(0);
    // 모든 카드가 실제 데이터다(샘플 카드 없음).
    await expect(cards.getByText('샘플')).toHaveCount(0);
    // 1280×800 에서 스크롤 없이 한 화면에 들어온다. SHY 요청(2026-10-25).
    await expectNoScroll(page);
    await page.screenshot({ path: 'screenshots/home.png', fullPage: true });
  });

  test('할 일이 없는 카드는 홈에 없다', async ({ page, request }) => {
    const cms = await (await request.get('/api/v1/home?business=cms')).json();
    expect(cms.kpis).toBeUndefined();
    for (const card of cms.cards) {
      expect(card.items.length).toBeGreaterThan(0);
    }
    // CMS 에는 진행 중인 정기 변경 주간이 없다(연락처 시드).
    await page.goto('/cms/home');
    await expect(page.getByTestId('home-card-SUSPENSIONS')).toBeVisible();
    await expect(page.getByTestId('home-card-UPDATE_CAMPAIGN')).toHaveCount(0);
  });

  test('카드의 모두 보기는 해당 기능 화면으로 간다', async ({ page }) => {
    await page.goto('/account-info/home');
    await page
      .getByTestId('home-card-OPINION_REPLIES')
      .getByRole('link', { name: '모두 보기' })
      .click();
    await expect(page).toHaveURL(/\/account-info\/opinions$/);
  });

  test('최근 알림은 상단 바의 알림 벨에서 보고 알림 센터로 간다', async ({ page }) => {
    await page.goto('/account-info/home');
    await page.getByRole('button', { name: /^알림/ }).click();
    await expect(page.getByText(/^읽지 않은 알림/)).toBeVisible();
    await page.getByRole('link', { name: '알림 센터에서 모두 보기' }).click();
    await expect(page).toHaveURL(/\/account-info\/notifications$/);
  });
});

test.describe('기관 담당자 홈', () => {
  // 빈 브라우저로 시작해 기관 담당자(김민준)로 로그인한다.
  test.use({
    storageState: { cookies: [], origins: [] },
    timezoneId: 'Asia/Seoul',
  });
  test.beforeEach(async ({ page }) => skipContactReconfirm(page));

  test('요청받은 일 카드를 한 줄에 두 장씩 놓고 일시중단은 없다', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: '김민준 (참가기관 · 현업)로 로그인' }).click();
    await expect(page).toHaveURL(/\/home$/);

    // 연락처·대화 카드는 앞선 시나리오(현행화 완료, 대화 읽음)에 따라 없을 수 있어 의견·테스트 카드로 본다.
    const cards = page.getByTestId('home-cards');
    const opinion = cards.getByTestId('home-card-OPINION_REPLIES');
    const testPlan = cards.getByTestId('home-card-TEST_PROGRESS');
    await expect(opinion.getByText('회신할 의견 요청')).toBeVisible();
    await expect(testPlan.getByText('요청받은 기관 테스트')).toBeVisible();
    await expect(cards.getByTestId('home-card-SUSPENSIONS')).toHaveCount(0);
    // 줄마다 기관이 할 일 단계를 보인다.
    await expect(opinion.getByText(/^미작성 · /).first()).toBeVisible();
    // 카드는 한 줄에 두 장씩(폭이 줄의 절반쯤)이고, 같은 줄 카드는 높이가 같다. SHY 요청(2026-10-24).
    const row = (await cards.boundingBox())!;
    const boxes = await Promise.all(
      (await cards.locator('[data-testid^="home-card-"]').all()).map(
        async (c) => (await c.boundingBox())!,
      ),
    );
    for (const box of boxes) {
      expect(box.width).toBeLessThan(row.width * 0.6);
    }
    for (const a of boxes) {
      for (const b of boxes) {
        if (Math.abs(a.y - b.y) < 2) {
          expect(Math.abs(a.height - b.height)).toBeLessThan(2);
        }
      }
    }
    await expectNoScroll(page);
    await page.screenshot({
      path: 'screenshots/home-organization.png',
      fullPage: true,
    });
  });
});
