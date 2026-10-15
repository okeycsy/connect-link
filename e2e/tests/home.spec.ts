import { expect, test } from '@playwright/test';

test.describe('홈 대시보드', () => {
  test('KPI 타일 네 개와 할 일 카드 여섯 장이 보이고 가장 급한 카드가 크다', async ({ page }) => {
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
      'data-featured',
      'true',
    );
    await expect(cards.getByText('샘플').first()).toBeVisible();
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
    ).toHaveAttribute('href', '/cms/contacts');
  });

  test('카드의 모두 보기는 해당 기능 화면으로 간다', async ({ page }) => {
    await page.goto('/account-info/home');
    await page
      .getByTestId('home-card-OPINION_REPLIES')
      .getByRole('link', { name: '모두 보기' })
      .click();
    await expect(page).toHaveURL(/\/account-info\/opinions$/);
  });
});
