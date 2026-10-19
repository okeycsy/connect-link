import { expect, test } from '@playwright/test';

test.describe('전역 검색 (Ctrl K)', () => {
  test('Ctrl K 로 팔레트를 열고 기관을 찾아 기준정보 패널로 간다', async ({ page }) => {
    await page.goto('/cms/home');
    await page.keyboard.press('Control+K');
    const palette = page.getByTestId('command-palette');
    await expect(palette).toBeVisible();
    // 검색어가 없으면 바로 가기만 보인다.
    await expect(palette.getByRole('option', { name: '연락처' })).toBeVisible();

    await palette.getByLabel('검색어').fill('신한');
    await expect(palette.getByRole('option', { name: '신한은행', exact: true })).toBeVisible();
    await expect(palette.getByRole('option', { name: '이준호' })).toBeVisible();
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/search-palette.png' });

    await palette.getByRole('option', { name: '신한은행', exact: true }).click();
    await expect(page).toHaveURL(/\/cms\/admin\/organizations\?org=\d+$/);
    // 팔레트가 닫히는 동안에는 대화상자가 둘이라, 닫힌 뒤 상세 패널을 본다.
    await expect(palette).toBeHidden();
    await expect(page.getByRole('dialog')).toContainText('088 · 참가기관');
  });

  test('상단 검색 상자로도 열리고, 결과가 없으면 안내가 뜬다', async ({ page }) => {
    await page.goto('/loan-transfer/home');
    await page.getByRole('button', { name: '전역 검색' }).click();
    const palette = page.getByTestId('command-palette');
    await palette.getByLabel('검색어').fill('zzzz');
    await expect(palette.getByText("'zzzz' 에 맞는 결과가 없습니다.")).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(palette).toBeHidden();
  });

  test('검색 API 는 레일의 업무 범위로 찾는다', async ({ request }) => {
    const cms = await (await request.get('/api/v1/search?business=cms&q=신한은행')).json();
    expect(cms.groups.some((g: { category: string }) => g.category === 'ORGANIZATION')).toBe(true);
    const loan = await (
      await request.get('/api/v1/search?business=loan-transfer&q=신한은행')
    ).json();
    expect(loan.groups.some((g: { category: string }) => g.category === 'ORGANIZATION')).toBe(
      false,
    );
  });
});
