import { expect, type Page, test } from '@playwright/test';

// 파일 반출 결재: 결제원 담당자(홍성민)가 공지에 올린 파일은 같은 팀 팀장(강지원)이 승인해야 참가기관(김민준)에 보인다.
test.use({
  storageState: { cookies: [], origins: [] },
  timezoneId: 'Asia/Seoul',
});

async function shot(page: Page, name: string) {
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.waitForTimeout(300);
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

async function loginAs(page: Page, name: string) {
  await page.goto('/login');
  await page.getByRole('button', { name: new RegExp(`^${name} \\(`) }).click();
  await expect(page).toHaveURL(/\/home$/);
  // 6개월 확인 창이 뜨면 닫는다(이 시나리오와 상관없다).
  await page.waitForTimeout(500);
  await page.keyboard.press('Escape');
}

async function openNotice(page: Page, title: string) {
  await page.goto('/account-info/notices');
  await page.getByTestId('notice-list').getByText(title).click();
}

const NOTICE = '본인계좌 일괄지급정지 전문 개선 일정 안내';

test('결제원 담당자가 올린 파일은 팀장이 승인한 뒤에 참가기관에 보인다', async ({ browser }) => {
  const fileName = `e2e-반출-${Date.now()}.txt`;

  const hong = await browser.newPage();
  await loginAs(hong, '홍성민');
  await openNotice(hong, NOTICE);
  const panel = hong.getByTestId('attachment-panel');
  await expect(panel.getByTestId('export-approval-note')).toBeVisible();
  await panel.locator('input[type="file"]').setInputFiles({
    name: fileName,
    mimeType: 'text/plain',
    buffer: Buffer.from('전문 변경 항목(샘플)'),
  });
  const row = panel.getByRole('listitem').filter({ hasText: fileName });
  await expect(row.getByTestId('approval-status')).toHaveText('결재 대기');
  await expect(row).toContainText('강지원 팀장 결재 뒤 참가기관에 보여요.');
  await shot(hong, 'file-approval-uploaded');

  // 결재 전: 참가기관 담당자에게는 보이지 않는다.
  const kim = await browser.newPage();
  await loginAs(kim, '김민준');
  await openNotice(kim, NOTICE);
  await expect(kim.getByTestId('attachment-panel')).not.toContainText(fileName);

  // 팀장이 파일 결재에서 승인한다.
  const kang = await browser.newPage();
  await loginAs(kang, '강지원');
  await kang
    .getByRole('link', { name: /파일 결재/ })
    .first()
    .click();
  await expect(kang).toHaveURL(/\/file-approvals$/);
  const item = kang.getByTestId('file-approval-row').filter({ hasText: fileName });
  await expect(item).toContainText('홍성민');
  await expect(item).toContainText(NOTICE);
  await shot(kang, 'file-approvals-leader');
  await item.getByRole('button', { name: `${fileName} 승인` }).click();
  await expect(kang.getByTestId('file-approvals')).not.toContainText(fileName);

  // 승인 뒤: 참가기관 담당자가 보고 내려받을 수 있다.
  await kim.reload();
  await expect(
    kim.getByTestId('attachment-panel').getByRole('link', { name: fileName }),
  ).toBeVisible();

  // 올린 사람은 내 파일 결재 현황에서 승인을 본다.
  await hong.goto('/account-info/file-approvals');
  await hong.getByText('내가 올린 파일').click();
  await expect(
    hong
      .getByTestId('file-approval-row')
      .filter({ hasText: fileName })
      .getByTestId('approval-status'),
  ).toHaveText('반출 승인');
});

test('팀장은 사유를 적어 반려한다', async ({ page }) => {
  await loginAs(page, '강지원');
  await page.goto('/account-info/file-approvals');
  // 시드의 결재 대기 파일(회의 안내문).
  const item = page
    .getByTestId('file-approval-row')
    .filter({ hasText: '전문_변경_회의_안내문.pdf' });
  await item.getByRole('button', { name: '전문_변경_회의_안내문.pdf 반려' }).click();
  const dialog = page.getByRole('dialog', { name: '반출 반려' });
  await dialog.getByRole('button', { name: '반려' }).click();
  await expect(dialog).toContainText('반려 사유를 적어 주세요.');
  await dialog.getByLabel('반려 사유').fill('회의 장소가 확정되면 다시 올려 주세요.');
  await shot(page, 'file-approvals-reject');
  await dialog.getByRole('button', { name: '반려' }).click();
  await expect(dialog).toBeHidden();
  await page.getByRole('group', { name: '상태' }).getByText('반려').click();
  await expect(
    page.getByTestId('file-approval-row').filter({ hasText: '전문_변경_회의_안내문.pdf' }),
  ).toContainText('회의 장소가 확정되면 다시 올려 주세요.');
});
