import { type APIRequestContext, expect, type Page, test } from '@playwright/test';

import { csrf, mailbox, skipContactReconfirm } from './support/account';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 결제원 담당자(홍성민)와 기관 담당자(김민준)로 로그인한다.
test.use({
  storageState: { cookies: [], origins: [] },
  timezoneId: 'Asia/Seoul',
});

const DEMO_PASSWORD = 'demo1234!';

/** 전체 화면 캡처. 위로 올린 뒤 찍어야 고정된 상단 바가 중간에 끼지 않는다. */
async function shot(page: Page, name: string) {
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

async function loginAs(page: Page, button: string) {
  await page.context().clearCookies();
  await page.goto('/login');
  await page.getByRole('button', { name: button }).click();
  await expect(page).toHaveURL(/\/home$/);
}

/** 서울 기준 오늘에서 days 만큼 뒤의 날짜(YYYY-MM-DD). */
function seoulDate(days: number) {
  return new Date(Date.now() + 9 * 3_600_000 + days * 86_400_000).toISOString().slice(0, 10);
}

/** 홍성민으로 KB국민은행 대상 계좌통합관리 기관 테스트를 API 로 만든다. request 의 세션은 홍성민으로 바뀐다. */
async function createAsHong(request: APIRequestContext, title: string): Promise<number> {
  const login = await request.post('/api/v1/auth/login', {
    data: { email: 'hong.sungmin@kftc.example.com', password: DEMO_PASSWORD },
    headers: await csrf(request),
  });
  expect(login.ok()).toBeTruthy();
  const organizations = (await (
    await request.get('/api/v1/test-plans/organizations?business=account-info')
  ).json()) as { id: number; code: string }[];
  const created = await request.post('/api/v1/test-plans', {
    data: {
      business: 'ACCOUNT_INFO',
      subBusiness: '계좌정보 조회·해지',
      title,
      description: '해지 요청 전문의 새 사유코드를 확인합니다.',
      startDate: seoulDate(0),
      endDate: seoulDate(7),
      items: [
        {
          title: '해지 요청 전문 처리 확인',
          description: '사유코드 07~09 를 처리하는지 확인합니다.',
        },
        { title: '해지 결과 통보 전문 확인', description: null },
      ],
      organizationIds: [organizations.find((o) => o.code === '004')!.id],
    },
    headers: await csrf(request),
  });
  expect(created.ok(), await created.text()).toBeTruthy();
  return ((await created.json()) as { id: number }).id;
}

test.describe('다. 기관 테스트', () => {
  test.beforeEach(async ({ page }) => skipContactReconfirm(page));

  test('결제원 담당자가 기관 테스트를 만들고 결과 등록 알림 대상을 정하면 대상 기관에 메일이 간다', async ({
    page,
    request,
  }) => {
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await page
      .getByRole('navigation')
      .getByRole('link', { name: '기관 테스트', exact: true })
      .click();
    await expect(page).toHaveURL(/\/account-info\/tests$/);
    await expect(page.getByRole('heading', { name: '기관 테스트' })).toBeVisible();
    const list = page.getByTestId('test-plan-table');
    await expect(list).toContainText('계좌정보 통합조회 전문 개선 2차');
    await expect(list).toContainText('기간 지남');
    await shot(page, 'test-plan-list');

    await page.getByRole('link', { name: '기관 테스트 만들기' }).click();
    await expect(page.getByRole('heading', { name: '기관 테스트 만들기' })).toBeVisible();
    const title = `E2E 해지 전문 개선 ${Date.now()}`;
    await page.getByRole('textbox', { name: '단위업무' }).fill('계좌정보 조회·해지');
    await page.getByRole('textbox', { name: '개발 건 이름' }).fill(title);
    await page
      .getByRole('textbox', { name: '테스트 내용·수행 가이드' })
      .fill('해지 요청 전문에 사유코드 07~09 를 더했습니다. 테스트 서버에서 확인해 주세요.');
    const items = page.getByTestId('item-card');
    await items.nth(0).getByRole('textbox', { name: '요구사항' }).fill('해지 요청 전문 처리 확인');
    await page.getByRole('button', { name: '요구사항 더하기' }).click();
    await items.nth(1).getByRole('textbox', { name: '요구사항' }).fill('해지 결과 통보 전문 확인');
    await items.nth(1).getByRole('textbox', { name: '확인 사항' }).fill('결과코드 0000 을 받는지');
    await page.getByRole('checkbox', { name: 'KB국민은행 대상' }).check();
    await page.getByRole('checkbox', { name: '삼성증권 대상' }).check();
    await expect(page.getByTestId('target-summary')).toHaveText('대상 2곳');
    await page.locator('input[type="file"]').setInputFiles({
      name: '테스트가이드.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4 sample'),
    });
    await shot(page, 'test-plan-editor');

    await page.getByRole('button', { name: '저장' }).click();
    await expect(page).toHaveURL(/\/account-info\/tests\/\d+$/);
    await expect(page.getByRole('heading', { name: title })).toBeVisible();
    await expect(page.getByTestId('test-plan-state')).toHaveText('진행 중');
    await expect(page.getByTestId('plan-progress')).toContainText('0%');
    const targets = page.getByTestId('target-table');
    await expect(targets).toContainText('KB국민은행');
    await expect(targets).toContainText('삼성증권');
    await shot(page, 'test-plan-detail');

    // 알림 설정: 만든 사람(홍성민)에 김태호을 더한다.
    await page.getByRole('link', { name: '알림 설정' }).click();
    await expect(page.getByRole('heading', { name: '결과 등록 알림 설정' })).toBeVisible();
    await expect(page.getByRole('checkbox', { name: '홍성민 알림 받기' })).toBeChecked();
    await page.getByRole('checkbox', { name: '김태호 알림 받기' }).check();
    await expect(page.getByTestId('recipient-count')).toHaveText('2명 선택');
    await shot(page, 'test-plan-recipients');
    await page.getByRole('button', { name: '저장' }).click();
    await expect(page).toHaveURL(/tab=content/);
    await expect(page.getByTestId('plan-recipients')).toContainText('김태호');
    await expect(page.getByText('테스트가이드.pdf')).toBeVisible();

    // 대상 기관 담당자에게 열림 메일이 간다.
    const mails = await mailbox(request, 'kim.minjun@kb.example.com');
    expect(mails.some((m) => m.subject.includes(`${title} 기관 테스트가 열렸습니다`))).toBeTruthy();
  });

  test('기관 담당자가 결과를 등록하면 수행률이 집계되고, 결제원 의견과 함께 모두 완료되면 테스트 수행 완료다', async ({
    page,
    request,
  }) => {
    const title = `E2E 해지 결과 통보 ${Date.now()}`;
    const id = await createAsHong(request, title);

    // 기관 담당자: 정상과 오류를 고른다. 오류는 특이사항(오류내용)이 있어야 저장된다.
    await loginAs(page, '김민준 (참가기관 · 현업)로 로그인');
    await page.goto(`/account-info/tests/${id}`);
    await expect(page.getByRole('heading', { name: title })).toBeVisible();
    await expect(page.getByTestId('target-table')).toHaveCount(0);
    const first = page.getByTestId('item-001');
    const second = page.getByTestId('item-002');
    await first.getByText('정상', { exact: true }).click();
    await second.getByText('오류', { exact: true }).click();
    await page.getByRole('button', { name: '결과 저장 (2)' }).click();
    await expect(second.getByText('오류면 특이사항에 오류내용을 써 주세요.')).toBeVisible();
    await second
      .getByRole('textbox', { name: '002 특이사항' })
      .fill('결과코드 0000 대신 9999 가 내려옵니다.');
    await page.getByRole('button', { name: '결과 저장 (2)' }).click();
    await expect(page.getByTestId('target-progress')).toHaveText('50%');
    await expect(page.getByTestId('plan-progress')).toContainText('50%');
    await shot(page, 'test-plan-results-organization');

    // 결제원 담당자: 알림을 받고, 001 은 완료 확인, 002 는 미니 수행률과 특이사항을 남긴다.
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    const notifications = (await (await page.request.get('/api/v1/notifications')).json()) as {
      content: { title: string }[];
    };
    expect(
      notifications.content.some((n) =>
        n.title.includes(`KB국민은행 계좌통합관리 ${title} 결과 등록`),
      ),
    ).toBeTruthy();
    await page.goto(`/account-info/tests/${id}`);
    await expect(page.getByTestId('awaiting-review')).toHaveText('확인할 결과 2');
    await page.getByTestId('item-001').getByRole('button', { name: '완료 확인' }).click();
    await expect(page.getByTestId('item-001').getByTestId('item-completed')).toBeVisible();
    await page.getByTestId('item-002').getByRole('button', { name: '002 의견' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('textbox', { name: '미니 수행률' }).fill('30');
    await dialog
      .getByRole('textbox', { name: '특이사항' })
      .fill('003 전문에 대해 예외사항 발생: 결과코드 매핑 확인이 필요합니다.');
    await page.waitForTimeout(300);
    await shot(page, 'test-plan-feedback');
    await dialog.getByRole('button', { name: '저장' }).click();
    await expect(dialog).toBeHidden();
    await expect(page.getByTestId('item-002').getByTestId('item-feedback')).toContainText(
      '미니 수행률 30%',
    );
    await expect(page.getByTestId('target-progress')).toHaveText('65%');
    await shot(page, 'test-plan-results-kftc');

    // 기관 담당자가 고쳐 다시 등록하면 결제원 의견은 "다시 확인" 이 된다.
    await loginAs(page, '김민준 (참가기관 · 현업)로 로그인');
    await page.goto(`/account-info/tests/${id}`);
    await page.getByTestId('item-002').getByText('정상', { exact: true }).click();
    await page
      .getByTestId('item-002')
      .getByRole('textbox', { name: '002 특이사항' })
      .fill('결과코드 매핑을 고쳐 다시 확인했습니다.');
    await page.getByRole('button', { name: '결과 저장 (1)' }).click();
    await expect(
      page.getByTestId('item-002').getByText('다시 확인', { exact: true }),
    ).toBeVisible();

    // 결제원 담당자가 다시 보고 완료 확인하면 모든 요구사항이 완료라 테스트 수행 완료다.
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await page.goto(`/account-info/tests/${id}`);
    await page.getByTestId('item-002').getByRole('button', { name: '완료 확인' }).click();
    await expect(page.getByTestId('test-plan-state')).toHaveText('수행 완료');
    await expect(page.getByTestId('plan-progress')).toContainText('100%');
    await expect(page.getByTestId('item-002').getByTestId('item-completed')).toHaveText(
      '테스트 수행 완료',
    );
    await shot(page, 'test-plan-completed');

    // 변경 이력: 기관별 결과 이력에 등록과 결제원 의견이 남는다.
    await page.getByRole('tab', { name: '변경 이력' }).click();
    await page.getByRole('textbox', { name: '이력 대상' }).click();
    await page.getByRole('option', { name: 'KB국민은행 결과' }).click();
    await expect(page.getByText('KB국민은행 결제원 의견').first()).toBeVisible();
    await expect(page.getByText('KB국민은행 결과 등록').first()).toBeVisible();
  });
});
