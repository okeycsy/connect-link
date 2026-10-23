import { expect, test, type Page } from '@playwright/test';

/** 필터 칩. Mantine Chip 의 input 은 숨겨져 있어 라벨 글자를 누른다. */
function chip(page: Page, group: string, label: string) {
  return page.getByRole('group', { name: group }).getByText(label, { exact: true });
}

test.describe('관리 > 기관·업무 기준정보', () => {
  test('관리 허브에서 기준정보로 들어가면 현재 업무의 기관이 보인다', async ({ page }) => {
    await page.goto('/loan-transfer/admin');
    await expect(page.getByRole('heading', { name: '관리' })).toBeVisible();
    const menu = page.getByTestId('admin-menu');
    await expect(menu.getByRole('link', { name: /가입 승인/ })).toBeVisible();
    // 정기 변경 주간은 연락처 메뉴 안에 있다.
    await expect(menu.getByRole('link', { name: /정기 변경 주간/ })).toHaveCount(0);
    await expect(menu.getByText('준비 중 · 공통')).toBeVisible();
    await page.screenshot({ path: 'screenshots/admin-hub.png', fullPage: true });

    await menu.getByRole('link', { name: /기관·업무 기준정보/ }).click();
    await expect(page).toHaveURL(/\/loan-transfer\/admin\/organizations$/);
    await expect(page.getByRole('heading', { name: '기관·업무 기준정보' })).toBeVisible();

    const table = page.getByTestId('organization-table');
    // 대출이동 참여 기관(사용 중)만: 금융결제원, KB국민은행, SBI저축은행, 우리은행, 카카오뱅크
    await expect(page.getByTestId('organization-count')).toHaveText('5곳');
    await expect(table.getByText('카카오뱅크')).toBeVisible();
    await expect(table.getByText('신한은행')).toHaveCount(0);
    await page.screenshot({ path: 'screenshots/admin-organizations.png', fullPage: true });
  });

  test('필터 칩과 검색으로 목록을 거른다', async ({ page }) => {
    await page.goto('/loan-transfer/admin/organizations');
    await expect(page.getByTestId('organization-count')).toHaveText('5곳');

    await chip(page, '업무', '전체 업무').click();
    await expect(page.getByTestId('organization-count')).toHaveText('11곳');

    await chip(page, '상태', '전체').click();
    await expect(page.getByTestId('organization-count')).toHaveText('12곳');
    await expect(page.getByTestId('organization-table').getByText('외환은행')).toBeVisible();

    await page.getByLabel('기관 검색').fill('저축');
    await expect(page.getByTestId('organization-count')).toHaveText('2곳');
    await expect(page.getByTestId('organization-table').getByText('저축은행중앙회')).toBeVisible();
  });

  test('행을 누르면 오른쪽 패널에서 참여 업무를 고쳐 저장한다', async ({ page }) => {
    await page.goto('/loan-transfer/admin/organizations');
    await chip(page, '업무', '전체 업무').click();
    await page.getByTestId('organization-table').getByRole('button', { name: '삼성증권' }).click();

    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('240 · 참가기관');
    await expect(dialog.getByLabel(/기관명/)).toHaveValue('삼성증권');
    await page.waitForTimeout(400);
    await page.screenshot({ path: 'screenshots/admin-organization-detail.png' });

    await dialog.getByLabel('대출이동').check();
    await dialog.getByRole('button', { name: '저장' }).click();
    await expect(page.getByText('기관 정보를 저장했습니다.')).toBeVisible();
    await expect(dialog).toContainText('(admin@kftc.example.com)');
    await dialog.getByRole('button', { name: '닫기' }).click();

    // 이 업무(대출이동)로 좁혀도 이제 삼성증권이 보인다.
    await chip(page, '업무', '대출').click();
    await expect(page.getByTestId('organization-table').getByText('삼성증권')).toBeVisible();
  });

  test('기관을 등록하면 목록에 바로 나타나고, 같은 코드는 서버 메시지를 보여 준다', async ({
    page,
  }) => {
    await page.goto('/cms/admin/organizations');
    await page.getByRole('button', { name: '기관 추가' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('기관 등록');

    await dialog.getByLabel(/기관 코드/).fill('004');
    await dialog.getByLabel(/기관명/).fill('중복 코드 기관');
    await dialog.getByRole('button', { name: '등록' }).click();
    await expect(page.getByText('이미 있는 기관 코드입니다: 004')).toBeVisible();

    await dialog.getByLabel(/기관 코드/).fill('e2e');
    await dialog.getByLabel(/기관명/).fill('이투이은행');
    await dialog.getByLabel('회사 이메일 도메인').fill('e2e.example.com');
    // 참가기관은 업권 없이 등록하지 못한다.
    await dialog.getByRole('button', { name: '등록' }).click();
    await expect(dialog.getByText('업권을 골라 주세요.')).toBeVisible();

    await dialog.getByRole('textbox', { name: /업권/ }).click();
    await page.getByRole('option', { name: '은행', exact: true }).click();
    await dialog.getByRole('button', { name: '등록' }).click();
    await expect(page.getByText('기관을 등록했습니다.')).toBeVisible();
    await expect(dialog).toContainText('E2E · 참가기관 · 은행');
    await expect(page.getByTestId('organization-table').getByText('이투이은행')).toBeVisible();
  });

  test('기준정보 관리 API 는 관리자 전용이고 공개 목록은 조회만 열린다', async ({ request }) => {
    const search = await request.get('/api/v1/organizations/search?business=cms');
    expect(search.ok()).toBeTruthy();
    const body = await search.json();
    expect(
      body.content.every((o: { businesses: { code: string }[] }) =>
        o.businesses.some((b) => b.code === 'CMS'),
      ),
    ).toBe(true);

    const docs = await request.get('/v3/api-docs');
    const paths = Object.keys((await docs.json()).paths);
    expect(paths).toContain('/api/v1/organizations/{id}');
  });
});

test.describe('관리 > 대상 업무 기준정보', () => {
  test('관리자가 일시중단 대상 업무를 찾고 더하면 등록 화면에서 고를 수 있다', async ({ page }) => {
    await page.goto('/account-info/admin');
    await page.getByTestId('admin-menu').getByRole('link', { name: /대상 업무 기준정보/ }).click();
    await expect(page).toHaveURL(/\/account-info\/admin\/target-works$/);
    const table = page.getByTestId('target-work-table');
    await expect(table.getByRole('button', { name: '타행환공동망' })).toContainText('없음');
    await page.screenshot({ path: 'screenshots/admin-target-works.png', fullPage: true });

    await page.getByLabel('대상 업무 검색').fill('지로');
    await expect(table.getByRole('button', { name: '타행환공동망' })).toHaveCount(0);
    await page.getByLabel('대상 업무 검색').fill('');

    const name = `E2E 대상 업무 ${Date.now()}`;
    await page.getByRole('button', { name: '대상 업무 추가' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel(/업무 이름/).fill(name);
    await dialog.getByLabel(/묶음/).fill('지급결제');
    await dialog.getByLabel('대출이동').check();
    await page.waitForTimeout(400);
    await page.screenshot({ path: 'screenshots/admin-target-work-new.png' });
    await dialog.getByRole('button', { name: '더하기' }).click();
    await expect(page.getByText('대상 업무를 더했습니다.')).toBeVisible();
    await expect(table.getByRole('button', { name })).toContainText('대출이동');

    // 더한 업무는 일시중단 등록에서 찾아 고를 수 있다.
    await page.goto('/account-info/suspensions/new');
    await page.getByRole('textbox', { name: '대상 업무 찾기' }).fill('E2E 대상');
    await expect(
      page.getByTestId('suspension-businesses').getByRole('checkbox', { name }),
    ).toBeVisible();
  });
});
