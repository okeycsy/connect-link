import { expect, test, type Page } from '@playwright/test';

/** 열린 기관 패널의 정보 탭에서 기관명을 바꿔 저장한다. */
async function rename(page: Page, name: string) {
  const dialog = page.getByRole('dialog');
  await dialog.getByRole('tab', { name: '정보' }).click();
  await dialog.getByLabel(/기관명/).fill(name);
  await dialog.getByRole('button', { name: '저장' }).click();
  // 앞서 저장한 알림이 아직 떠 있을 수 있어 가장 최근 알림을 본다.
  await expect(page.getByText('기관 정보를 저장했습니다.').last()).toBeVisible();
  await expect(dialog).toContainText(`(admin@kftc.example.com)`);
}

test.describe('첨부파일과 변경 이력', () => {
  test('데모 도구의 첨부파일 화면에서 올리고 내려받고 지운다', async ({ page, request }) => {
    await page.goto('/cms/home');
    await page.getByRole('button', { name: '데모 도구' }).click();
    await page.getByRole('menuitem', { name: '첨부파일' }).click();
    await expect(page).toHaveURL(/\/cms\/demo\/attachments$/);
    await expect(page.getByRole('heading', { name: '첨부파일' })).toBeVisible();

    const name = `e2e-공문-${Date.now()}.txt`;
    await page.locator('input[type="file"]').setInputFiles({
      name,
      mimeType: 'text/plain',
      buffer: Buffer.from('가상 공문 내용', 'utf-8'),
    });
    await expect(page.getByText(`${name} 을(를) 올렸습니다.`)).toBeVisible();
    const link = page.getByRole('link', { name });
    await expect(link).toBeVisible();
    await expect(page.getByTestId('attachment-panel')).toContainText('admin@kftc.example.com');
    await page.screenshot({ path: 'screenshots/demo-attachments.png', fullPage: true });

    // 내려받기 주소는 원래 이름과 내용을 돌려준다.
    const href = await link.getAttribute('href');
    const download = await request.get(href!);
    expect(download.ok()).toBeTruthy();
    expect(download.headers()['content-disposition']).toContain("filename*=UTF-8''");
    expect(await download.text()).toBe('가상 공문 내용');

    await page.getByRole('button', { name: `${name} 지우기` }).click();
    await page.getByRole('button', { name: '지우기', exact: true }).click();
    await expect(page.getByText(`${name} 을(를) 지웠습니다.`)).toBeVisible();
    await expect(link).toHaveCount(0);
  });

  test('기관 상세의 변경 이력 탭에 저장한 변경이 변경 전·후로 쌓인다', async ({ page }) => {
    await page.goto('/cms/admin/organizations');
    await page
      .getByTestId('organization-table')
      .getByRole('button', { name: '신협' })
      .click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('tab', { name: '변경 이력' }).click();
    const history = dialog.getByTestId('change-history');
    // 시드 이력: 이름과 이메일 도메인을 바꾼 수정 1건.
    await expect(history).toContainText('신협 수정');
    await expect(history).toContainText('나다신용협동조합');
    await expect(history).toContainText('nada-cu.example.com');
    await page.waitForTimeout(400);
    await page.screenshot({ path: 'screenshots/organization-history.png' });

    const newName = `신협(${Date.now() % 1000})`;
    await rename(page, newName);
    await dialog.getByRole('tab', { name: '변경 이력' }).click();
    const latest = dialog.getByTestId('change-history').locator('.mantine-Timeline-item').first();
    await expect(latest).toContainText(`${newName} 수정`);
    await expect(latest).toContainText('기관명');
    await expect(latest).toContainText(newName);
    await expect(latest).toContainText('데모 관리자 (admin@kftc.example.com)');

    // 다른 시나리오가 원래 이름에 기대므로 되돌려 둔다(이력이 한 건 더 남는다).
    await rename(page, '신협');
    await dialog.getByRole('button', { name: '닫기' }).click();
    await expect(page.getByTestId('organization-table').getByText('신협')).toBeVisible();
  });

  test('변경 이력 API 는 권한 빈이 없는 대상을, 첨부 API 는 없는 일시중단을 거부한다', async ({
    request,
  }) => {
    const history = await request.get('/api/v1/change-histories?targetType=CONTACT&targetId=1');
    expect(history.status()).toBe(403);
    const attachments = await request.get(
      '/api/v1/attachments?ownerType=SUSPENSION&ownerId=999999',
    );
    expect(attachments.status()).toBe(403);
  });
});
