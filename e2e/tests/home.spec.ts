import { expect, test } from '@playwright/test';

test.describe('홈 대시보드', () => {
  test('건수 요약 없이 할 일이 있는 카드만 보이고, 일시중단은 오늘부터 다가오는 목록이다', async ({
    page,
  }) => {
    await page.goto('/account-info/home');
    await expect(page.getByRole('heading', { name: '안녕하세요, 데모 관리자 님' })).toBeVisible();

    const cards = page.getByTestId('home-cards');
    for (const key of ['CONTACT_CONFIRM', 'OPINION_REPLIES', 'TEST_PROGRESS', 'SUSPENSIONS']) {
      await expect(cards.getByTestId(`home-card-${key}`)).toBeVisible();
    }
    await expect(cards.getByTestId('home-card-CONTACT_CONFIRM')).toHaveAttribute(
      'data-compact',
      'true',
    );
    // 건수 요약 타일은 없다.
    await expect(page.getByText('진행 중 기관 테스트', { exact: true })).toHaveCount(1);
    // 왼쪽 큰 칸은 다가오는 일시중단(요일 칸 없음, 날짜별 묶음), 오른쪽은 회신 대기 의견 요청이다.
    // 최근 알림은 홈에 두지 않고 상단 바의 알림 벨에서 본다.
    const upcoming = cards.getByTestId('suspension-upcoming');
    await expect(upcoming).toBeVisible();
    await expect(upcoming.getByText(/^오늘 · /)).toBeVisible();
    await expect(cards.getByText('최근 소식')).toHaveCount(0);
    const suspensionBox = (await cards.getByTestId('home-card-SUSPENSIONS').boundingBox())!;
    const opinionBox = (await cards.getByTestId('home-card-OPINION_REPLIES').boundingBox())!;
    expect(suspensionBox.x).toBeLessThan(opinionBox.x);
    expect(suspensionBox.height).toBeLessThan(640);
    // 모든 카드가 실제 데이터다(샘플 카드 없음).
    await expect(cards.getByText('샘플')).toHaveCount(0);
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
    await expect(page.getByTestId('home-card-CONTACT_CONFIRM')).toBeVisible();
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
