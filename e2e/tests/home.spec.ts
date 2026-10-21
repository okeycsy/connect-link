import { expect, test } from '@playwright/test';

test.describe('홈 대시보드', () => {
  test('KPI 타일, 최근 소식, 이번 주 일시중단과 카드 여섯 장이 보이고 확인 필요 담당자는 작다', async ({
    page,
  }) => {
    await page.goto('/account-info/home');
    await expect(page.getByRole('heading', { name: '안녕하세요, 데모 관리자 님' })).toBeVisible();

    const cards = page.getByTestId('home-cards');
    for (const key of [
      'CONTACT_CONFIRM',
      'UPDATE_CAMPAIGN',
      'OPINION_REPLIES',
      'TEST_PROGRESS',
      'SUSPENSIONS',
      'SIGNUP_APPROVAL',
    ]) {
      await expect(cards.getByTestId(`home-card-${key}`)).toBeVisible();
    }
    await expect(cards.getByTestId('home-card-CONTACT_CONFIRM')).toHaveAttribute(
      'data-compact',
      'true',
    );
    // 왼쪽 큰 칸은 이번 주 일시중단(월~금 요일 칸), 오른쪽은 회신 대기 의견 요청이고 한 화면에 들어온다.
    // 알림 센터와 같은 최근 소식은 맨 아래에 둔다.
    await expect(cards.getByTestId('suspension-week')).toBeVisible();
    await expect(cards.getByTestId('home-recent-notifications')).toBeVisible();
    const suspensionBox = (await cards.getByTestId('home-card-SUSPENSIONS').boundingBox())!;
    const opinionBox = (await cards.getByTestId('home-card-OPINION_REPLIES').boundingBox())!;
    const recentBox = (await cards.getByTestId('home-recent-notifications').boundingBox())!;
    expect(suspensionBox.x).toBeLessThan(opinionBox.x);
    expect(suspensionBox.height).toBeLessThan(600);
    expect(recentBox.y).toBeGreaterThan(suspensionBox.y + suspensionBox.height);
    // 모든 카드가 실제 데이터다(샘플 카드 없음).
    await expect(cards.getByText('샘플')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/home.png', fullPage: true });
  });

  test('레일을 바꾸면 홈 숫자가 업무 영역에 맞게 바뀐다', async ({ page, request }) => {
    const account = await (await request.get('/api/v1/home?business=account-info')).json();
    const cms = await (await request.get('/api/v1/home?business=cms')).json();
    expect(account.kpis[0].value).not.toBe(cms.kpis[0].value);

    await page.goto('/account-info/home');
    await page.getByRole('navigation').getByRole('link', { name: 'CMS' }).click();
    await expect(page).toHaveURL(/\/cms\/home$/);
    await expect(page.getByText('CMS', { exact: true }).first()).toBeVisible();
    await expect(
      page.getByTestId('home-card-UPDATE_CAMPAIGN').getByRole('link', { name: '모두 보기' }),
    ).toHaveAttribute('href', /^\/cms\/contacts\/update-campaigns/);
  });

  test('카드의 모두 보기는 해당 기능 화면으로 간다', async ({ page }) => {
    await page.goto('/account-info/home');
    await page
      .getByTestId('home-card-OPINION_REPLIES')
      .getByRole('link', { name: '모두 보기' })
      .click();
    await expect(page).toHaveURL(/\/account-info\/opinions$/);
  });

  test('최근 소식은 4건 이상 채워 보이고 더보기는 알림 센터로 간다', async ({ page }) => {
    await page.goto('/account-info/home');
    const recent = page.getByTestId('home-recent-notifications');
    await expect(recent.getByRole('button').nth(3)).toBeVisible();
    await recent.getByRole('link', { name: '더보기' }).click();
    await expect(page).toHaveURL(/\/account-info\/notifications$/);
  });
});
