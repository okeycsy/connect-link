import { expect, test } from '@playwright/test';

test.describe('알림 센터와 발송함', () => {
  test('벨에 읽지 않은 알림이 표시되고 알림 센터로 이어진다', async ({ page }) => {
    await page.goto('/account-info/home');
    const bell = page.getByRole('button', { name: /^알림 \d+건$/ });
    await expect(bell).toBeVisible();
    await bell.click();
    await expect(page.getByText(/읽지 않은 알림 \d+건/)).toBeVisible();
    // 팝오버 페이드인(150ms)이 끝난 뒤 찍는다.
    await page.waitForTimeout(400);
    await page.screenshot({ path: 'screenshots/notification-bell.png' });

    await page.getByRole('link', { name: '알림 센터에서 모두 보기' }).click();
    await expect(page).toHaveURL(/\/account-info\/notifications$/);
    await expect(page.getByRole('heading', { name: '알림 센터' })).toBeVisible();
    const list = page.getByTestId('notification-list');
    await expect(list.getByText('SBI저축은행 정마바 님이 가입을 신청했습니다')).toBeVisible();
    await page.screenshot({
      path: 'screenshots/notification-center.png',
      fullPage: true,
    });
  });

  test('알림을 누르면 읽음이 되고 연결된 화면으로 간다', async ({ page }) => {
    await page.goto('/account-info/notifications');
    const header = page.getByText(/읽지 않은 알림 (\d+)건/);
    await expect(header).toBeVisible();
    const before = await header.textContent();
    const countBefore = Number(before?.match(/(\d+)건/)?.[1]);
    expect(countBefore).toBeGreaterThan(0);

    await page
      .getByTestId('notification-list')
      .getByRole('button', { name: '신한은행 CMS 일시중단이 확정되었습니다' })
      .click();
    await expect(page).toHaveURL(/\/cms\/suspensions$/);

    // 다른 시나리오의 가입 신청이 같은 때 관리자 알림을 늘릴 수 있어 건수 대신 그 알림이 읽음이 됐는지 본다.
    await page.goto('/cms/notifications');
    await expect(page.getByText(/읽지 않은 알림 \d+건/)).toBeVisible();
    const list = await (await page.request.get('/api/v1/notifications?size=50')).json();
    const clicked = list.content.find(
      (n: { title: string }) => n.title === '신한은행 CMS 일시중단이 확정되었습니다',
    );
    expect(clicked.read).toBe(true);
  });

  test('데모 도구 메뉴에서 발송함을 연다', async ({ page }) => {
    await page.goto('/account-info/home');
    await page.getByRole('button', { name: '데모 도구' }).click();
    await page.getByRole('menuitem', { name: '발송함' }).click();
    await expect(page).toHaveURL(/\/account-info\/demo\/outbox$/);
    await expect(page.getByRole('heading', { name: '발송함' })).toBeVisible();
    const table = page.getByTestId('outbox-table');
    await expect(table.getByText('@admin')).toBeVisible();
    await expect(table.getByText('발송 대기').first()).toBeVisible();
    await page.screenshot({
      path: 'screenshots/notification-outbox.png',
      fullPage: true,
    });
  });

  test('알림 API 는 본인 것만 다룬다 (데모 관리자 세션)', async ({ request }) => {
    const me = await request.get('/api/v1/me');
    expect(me.ok()).toBeTruthy();
    expect((await me.json()).admin).toBe(true);

    const list = await request.get('/api/v1/notifications?unreadOnly=true');
    expect(list.ok()).toBeTruthy();
    expect((await list.json()).content.every((n: { read: boolean }) => !n.read)).toBe(true);
  });
});
