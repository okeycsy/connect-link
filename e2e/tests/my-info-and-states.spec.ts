import { expect, test } from '@playwright/test';

test.describe('내 정보와 상태 화면', () => {
  test('사용자 메뉴에서 내 정보로 가면 계정과 접속 망이 보인다', async ({ page }) => {
    await page.goto('/account-info/home');
    await page.getByRole('button', { name: '사용자 메뉴' }).click();
    await page.getByRole('menuitem', { name: '내 정보' }).click();
    await expect(page).toHaveURL(/\/account-info\/me$/);
    await expect(page.getByRole('heading', { name: '내 정보' })).toBeVisible();
    const account = page.getByTestId('my-account');
    await expect(account.getByText('데모 관리자')).toBeVisible();
    await expect(account.getByText('admin@kftc.example.com')).toBeVisible();
    await expect(account.getByText('내부망')).toBeVisible();
    await expect(
      page.getByTestId('my-contact').getByText('연락처에 등록된 내 담당자 정보가 없습니다.'),
    ).toBeVisible();
    await page.screenshot({ path: 'screenshots/my-info.png', fullPage: true });
  });

  test('데모 도구의 망 전환은 서버가 보는 망을 바꾼다', async ({ page }) => {
    await page.goto('/account-info/home');
    await expect(page.getByTestId('network-zone-badge')).toHaveCount(0);
    await page.getByRole('button', { name: '데모 도구' }).click();
    await page.getByRole('menuitem', { name: /망 전환: 내부망 → 인터넷망/ }).click();
    await expect(page.getByTestId('network-zone-badge')).toHaveText('인터넷망');

    await page.goto('/account-info/me');
    await expect(page.getByTestId('my-account').getByText('인터넷망')).toBeVisible();

    await page.getByRole('button', { name: '데모 도구' }).click();
    await page.getByRole('menuitem', { name: /망 전환: 인터넷망 → 내부망/ }).click();
    await expect(page.getByTestId('network-zone-badge')).toHaveCount(0);
  });

  test('모르는 기능 주소는 주소 없음 안내를, 데모 도구는 상태 화면 모음을 보여 준다', async ({
    page,
  }) => {
    await page.goto('/cms/nope');
    await expect(page.getByTestId('state-notFound')).toBeVisible();
    await page.getByRole('link', { name: '홈으로' }).click();
    await expect(page).toHaveURL(/\/cms\/home$/);

    await page.getByRole('button', { name: '데모 도구' }).click();
    await page.getByRole('menuitem', { name: '상태 화면' }).click();
    await expect(page).toHaveURL(/\/cms\/demo\/states$/);
    const states = page.getByTestId('demo-states');
    for (const kind of ['empty', 'error', 'forbidden', 'notFound', 'internalOnly']) {
      await expect(states.getByTestId(`state-${kind}`)).toBeVisible();
    }
    await page.screenshot({ path: 'screenshots/demo-states.png', fullPage: true });
  });
});
