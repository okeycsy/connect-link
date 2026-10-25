import { expect, type Page, test } from '@playwright/test';

// 기관 한눈에 보기: 결제원 담당자(홍성민)가 기관을 골라 담당자·의견 요청·기관 테스트·일시중단을 한 화면에서 본다.
test.use({
  storageState: { cookies: [], origins: [] },
  timezoneId: 'Asia/Seoul',
});

async function shot(page: Page, name: string) {
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.waitForTimeout(300);
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

async function loginAs(page: Page, button: string) {
  await page.goto('/login');
  await page.getByRole('button', { name: button }).click();
  await expect(page).toHaveURL(/\/home$/);
}

test('결제원 담당자는 기관을 골라 한눈에 본다', async ({ page }) => {
  await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
  await page.goto('/account-info/organizations');
  await expect(page.getByText('기관을 골라 주세요')).toBeVisible();

  await page.getByRole('textbox', { name: '기관 고르기' }).click();
  await page.getByRole('option', { name: /KB국민은행/ }).click();
  await expect(page).toHaveURL(/\/account-info\/organizations\/\d+$/);

  await expect(page.getByTestId('org-overview-CONTACT')).toContainText('김민준');
  await expect(page.getByTestId('org-overview-OPINION_REQUEST')).toBeVisible();
  await expect(page.getByTestId('org-overview-TEST_PLAN')).toBeVisible();
  await expect(page.getByTestId('org-overview-SUSPENSION')).toBeVisible();
  await shot(page, 'organization-overview');
});
