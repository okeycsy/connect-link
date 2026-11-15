import { expect, type Page, test } from '@playwright/test';

// 파일 반출 결재: 결제원 담당자(홍성민)가 공지에 붙인 파일은 같은 팀 팀장(강지원)이 알림 센터에서 승인해야 참가기관(김민준)에 보인다.
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
const REQUESTED = '홍성민 님이 첨부파일 반출 결재를 요청했습니다';

test('결제원 담당자가 붙인 파일은 팀장이 알림 센터에서 승인한 뒤에 참가기관에 보인다', async ({
  browser,
}) => {
  const stamp = Date.now();
  const files = [`e2e-반출-${stamp}-1.txt`, `e2e-반출-${stamp}-2.txt`];

  const hong = await browser.newPage();
  await loginAs(hong, '홍성민');
  await openNotice(hong, NOTICE);
  const panel = hong.getByTestId('attachment-panel');
  await expect(panel.getByTestId('export-approval-note')).toBeVisible();
  for (const name of files) {
    await panel.locator('input[type="file"]').setInputFiles({
      name,
      mimeType: 'text/plain',
      buffer: Buffer.from('전문 변경 항목(샘플)'),
    });
    const row = panel.getByRole('listitem').filter({ hasText: name });
    await expect(row.getByTestId('approval-status')).toHaveText('결재 대기');
  }
  await expect(panel.getByRole('listitem').filter({ hasText: files[0] })).toContainText(
    '강지원 팀장 결재 뒤 참가기관에 보여요.',
  );
  await shot(hong, 'file-approval-uploaded');

  // 결재 전: 참가기관 담당자에게는 보이지 않는다.
  const kim = await browser.newPage();
  await loginAs(kim, '김민준');
  await openNotice(kim, NOTICE);
  await expect(kim.getByTestId('attachment-panel')).not.toContainText(files[0]);

  // 팀장: 알림 센터의 결재 요청 알림(등록 한 건에 하나)을 누르면 결재 창이 열린다.
  const kang = await browser.newPage();
  await loginAs(kang, '강지원');
  await kang.goto('/account-info/notifications');
  await shot(kang, 'file-approval-notifications');
  await kang
    .getByTestId('notification-list')
    .getByRole('button', { name: REQUESTED })
    .first()
    .click();
  const dialog = kang.getByRole('dialog', { name: '첨부파일 반출 결재' });
  await expect(dialog).toContainText(NOTICE);
  await expect(dialog).toContainText('계좌서비스부 계좌통합팀');
  for (const name of files) {
    await expect(dialog.getByTestId('approval-file').filter({ hasText: name })).toBeVisible();
  }
  await shot(kang, 'file-approval-dialog');
  await dialog.getByRole('button', { name: /모두 승인/ }).click();
  await expect(dialog).toBeHidden();

  // 승인 뒤: 참가기관 담당자가 보고 내려받을 수 있다.
  await kim.reload();
  for (const name of files) {
    await expect(kim.getByTestId('attachment-panel').getByRole('link', { name })).toBeVisible();
  }

  // 올린 사람은 파일 옆 배지로 승인을 본다.
  await hong.reload();
  await expect(
    hong
      .getByTestId('attachment-panel')
      .getByRole('listitem')
      .filter({ hasText: files[0] })
      .getByTestId('approval-status'),
  ).toHaveText('반출 승인');
});

test('팀장은 사유를 적어 반려한다', async ({ page }) => {
  await loginAs(page, '강지원');
  await page.goto('/account-info/notifications');
  // 시드의 결재 대기 건(회의 개최 안내).
  await page
    .getByTestId('notification-list')
    .getByRole('button', { name: REQUESTED })
    .last()
    .click();
  const dialog = page.getByRole('dialog', { name: '첨부파일 반출 결재' });
  await expect(dialog).toContainText('전문_변경_회의_안내문.pdf');
  await dialog.getByRole('button', { name: '반려' }).click();
  await dialog.getByRole('button', { name: '반려' }).click();
  await expect(dialog).toContainText('반려 사유를 적어 주세요.');
  await dialog.getByLabel('반려 사유').fill('회의 장소가 확정되면 다시 올려 주세요.');
  await dialog.getByRole('button', { name: '반려' }).click();
  await expect(dialog).toBeHidden();
  // 다시 열면 결재가 끝난 건이다.
  await page
    .getByTestId('notification-list')
    .getByRole('button', { name: REQUESTED })
    .last()
    .click();
  await expect(dialog).toContainText('이미 결재한 건이에요.');
  await expect(dialog).toContainText('회의 장소가 확정되면 다시 올려 주세요.');
});
