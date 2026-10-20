import { expect, type Page, test } from '@playwright/test';

// 휴대폰 폭(390px)에서 앱 셸과 목록 화면을 본다. 공통 세션(데모 관리자)으로 시작한다.
test.use({ viewport: { width: 390, height: 844 }, hasTouch: true });

/** 전체 화면 캡처. 위로 올린 뒤 찍어야 고정된 상단 바가 중간에 끼지 않는다. */
async function shot(page: Page, name: string) {
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.waitForTimeout(300);
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

/** 가로로 넘쳐 옆으로 밀리는 화면이 없어야 한다. */
async function expectNoHorizontalScroll(page: Page) {
  const overflow = await page.evaluate(
    () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
  );
  expect(overflow).toBeLessThanOrEqual(0);
}

test('휴대폰 폭에서는 메뉴가 접혀 있고 메뉴 버튼으로 열어 이동한다', async ({ page }) => {
  await page.goto('/account-info/home');
  const nav = page.getByRole('navigation');
  const contactsLink = nav.getByRole('link', { name: '연락처' });
  await expect(page.getByRole('heading', { name: /안녕하세요/ })).toBeVisible();
  await expect(contactsLink).not.toBeInViewport();
  await expectNoHorizontalScroll(page);
  await shot(page, 'mobile-home');

  await page.getByRole('button', { name: '메뉴 열기' }).click();
  await expect(contactsLink).toBeInViewport();
  await shot(page, 'mobile-menu');

  await contactsLink.click();
  await expect(page).toHaveURL(/\/account-info\/contacts$/);
  await expect(contactsLink).not.toBeInViewport();
  await expect(page.getByRole('button', { name: '메뉴 열기' })).toBeVisible();
});

test('휴대폰 폭에서는 표가 카드 목록으로 보인다', async ({ page }) => {
  await page.goto('/account-info/contacts');
  const table = page.getByTestId('contact-table');
  await expect(table).toBeVisible();
  // 머리 줄은 숨기고 칸마다 열 이름을 붙인다.
  await expect(table.locator('thead')).toBeHidden();
  await expect(table.locator('td[data-label="휴대폰"]').first()).toBeVisible();
  await expectNoHorizontalScroll(page);
  await shot(page, 'mobile-contacts');

  await page.goto('/account-info/opinions');
  await expect(page.getByTestId('opinion-table')).toBeVisible();
  await expectNoHorizontalScroll(page);
  await shot(page, 'mobile-opinions');

  await page.goto('/account-info/tests');
  await expect(page.getByTestId('test-plan-table')).toBeVisible();
  await expectNoHorizontalScroll(page);
});

test('휴대폰 폭에서 검색은 상단 바 아이콘으로 연다', async ({ page }) => {
  await page.goto('/account-info/home');
  await page.getByRole('button', { name: '전역 검색' }).click();
  await expect(page.getByRole('dialog')).toBeVisible();
});
