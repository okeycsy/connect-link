import { type APIRequestContext, expect, type Page, test } from '@playwright/test';

import { csrf, mailbox } from './support/account';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 기관 담당자(김가나)와 결제원 담당자(홍길동)로 로그인한다.
// 달력이 서버와 같은 날짜를 보이도록 브라우저 시간대를 서버(Asia/Seoul)에 맞춘다.
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
  await page.goto('/login');
  await page.getByRole('button', { name: button }).click();
  await expect(page).toHaveURL(/\/home$/);
}

/** 서울 기준 오늘에서 days 만큼 뒤의 날짜(연, 월, 일). */
function seoulDate(days: number) {
  const d = new Date(Date.now() + 9 * 3_600_000 + days * 86_400_000);
  return {
    year: d.getUTCFullYear(),
    month: d.getUTCMonth() + 1,
    day: d.getUTCDate(),
  };
}

function iso(date: { year: number; month: number; day: number }, time: string) {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.year}-${pad(date.month)}-${pad(date.day)}T${time}:00`;
}

/** 일시 칸을 눌러 달력에서 날짜를 고르고 시·분을 넣는다. 다음 달이면 달을 넘긴다. */
async function pickDateTime(
  page: Page,
  label: string,
  date: { year: number; month: number; day: number },
  hour: string,
  minute: string,
) {
  await page.getByRole('button', { name: label, exact: true }).click();
  const dropdown = page.getByRole('dialog').filter({ has: page.getByRole('spinbutton') });
  const dayButton = dropdown.getByRole('button', {
    name: `${date.day} ${date.month}월 ${date.year}`,
    exact: true,
  });
  if (!(await dayButton.isVisible())) {
    // 머리의 세 버튼은 이전 달, 달 이름, 다음 달 순서다.
    await dropdown.getByRole('button').nth(2).click();
  }
  await dayButton.click();
  await dropdown.getByRole('spinbutton').first().click();
  await page.keyboard.type(hour + minute);
  await dropdown.getByRole('button').last().click();
  await expect(dropdown).toBeHidden();
}

/** 김가나로 가나은행 계좌통합관리 일시중단을 API 로 등록한다. request 의 세션은 김가나로 바뀐다. */
async function registerAsGana(request: APIRequestContext, reason: string): Promise<number> {
  const login = await request.post('/api/v1/auth/login', {
    data: { email: 'kim.gana@gana.example.com', password: DEMO_PASSWORD },
    headers: await csrf(request),
  });
  expect(login.ok()).toBeTruthy();
  const date = seoulDate(4);
  const created = await request.post('/api/v1/suspensions', {
    data: {
      business: 'ACCOUNT_INFO',
      organizationId: null,
      subBusiness: '계좌정보 통합조회',
      startAt: iso(date, '01:00'),
      endAt: iso(date, '03:00'),
      reason,
      kftcAction: '작업 시간 동안 가나은행 조회 요청을 차단해 주세요.',
      contactName: '김가나',
      contactPhone: '010-0000-2101',
      contactEmail: 'kim.gana@gana.example.com',
      extraNote: null,
    },
    headers: await csrf(request),
  });
  expect(created.ok(), await created.text()).toBeTruthy();
  return ((await created.json()) as { id: number }).id;
}

test.describe('라. 일시중단', () => {
  test('기관 담당자가 양식으로 일시중단을 등록하면 검토 대기가 되고 결제원 담당자에게 메일이 간다', async ({
    page,
    request,
  }) => {
    await loginAs(page, '김가나 (참가기관 · 현업)로 로그인');
    await page.getByRole('navigation').getByRole('link', { name: '일시중단', exact: true }).click();
    await expect(page).toHaveURL(/\/account-info\/suspensions$/);
    await expect(page.getByRole('heading', { name: '일시중단' })).toBeVisible();
    // 기관 담당자는 자기 기관 것만 보고, 기관 고르기는 없다.
    const table = page.getByTestId('suspension-table');
    await expect(table).toContainText('가나은행');
    await expect(table).not.toContainText('다라은행');
    await expect(page.getByRole('textbox', { name: '기관' })).toHaveCount(0);
    await shot(page, 'suspension-list-organization');

    await page.getByRole('link', { name: '일시중단 등록' }).click();
    await expect(page.getByRole('heading', { name: '일시중단 등록' })).toBeVisible();
    await expect(page.getByRole('textbox', { name: '기관' })).toHaveValue('가나은행');
    await expect(page.getByRole('textbox', { name: '이름' })).toHaveValue('김가나');

    const reason = `E2E 계정계 패치 작업 ${Date.now()}`;
    const date = seoulDate(3);
    await page.getByRole('textbox', { name: '세부업무' }).fill('계좌정보 통합조회');
    await pickDateTime(page, '중단 시작', date, '02', '00');
    await pickDateTime(page, '중단 종료', date, '04', '30');
    await page.getByRole('textbox', { name: '중단 이유(작업 내용)' }).fill(reason);
    await page
      .getByRole('textbox', { name: '결제원 조치사항' })
      .fill('작업 시간 동안 가나은행 조회 요청을 차단해 주세요.');
    await page.getByRole('textbox', { name: '연락처' }).fill('010-0000-2101');
    await page.locator('input[type="file"]').setInputFiles({
      name: '작업공문.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4 sample'),
    });
    await expect(page.getByRole('list', { name: '올릴 파일' })).toContainText('작업공문.pdf');
    await shot(page, 'suspension-editor');

    await page.getByRole('button', { name: '저장' }).click();
    await expect(page).toHaveURL(/\/account-info\/suspensions\/\d+$/);
    await expect(page.getByRole('heading', { name: '가나은행 일시중단' })).toBeVisible();
    await expect(page.getByTestId('suspension-status')).toHaveText('검토 대기');
    await expect(page.getByTestId('suspension-period')).toContainText('02:00 ~ 04:30');
    await expect(page.getByText('작업공문.pdf')).toBeVisible();
    // 기관 담당자는 확인·확정은 못 하고, 자기가 올린 건은 고치고 지울 수 있다.
    await expect(page.getByRole('button', { name: '확인 시작' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: '고치기' })).toBeVisible();
    await expect(page.getByRole('button', { name: '삭제' })).toBeVisible();
    await shot(page, 'suspension-detail-organization');

    // 그 업무 결제원 담당자에게 등록 메일이 간다(매터모스트는 발송함에 함께 남는다).
    const mails = await mailbox(request, 'hong.gildong@kftc.example.com');
    expect(
      mails.some((m) => m.subject.includes('가나은행 계좌통합관리 일시중단이 등록되었습니다')),
    ).toBeTruthy();
  });

  test('결제원 담당자가 확인을 시작하고 조치 및 인수인계 사항을 써서 확정하고, 기관별 이력과 취합을 본다', async ({
    page,
    request,
  }) => {
    const reason = `E2E 인터넷뱅킹 점검 ${Date.now()}`;
    const id = await registerAsGana(request, reason);

    await loginAs(page, '홍길동 (결제원 · 현업)로 로그인');
    await page.getByRole('navigation').getByRole('link', { name: '일시중단', exact: true }).click();
    await expect(page.getByTestId('pending-review-count')).toContainText('확정 전');
    // 진행 중에는 전 기관이 보인다.
    await expect(page.getByTestId('suspension-table')).toContainText('가나은행');
    await shot(page, 'suspension-list');

    await page
      .getByRole('group', { name: '시기' })
      .getByText(/예정 \d/)
      .click();
    await expect(page).toHaveURL(/period=UPCOMING/);
    const table = page.getByTestId('suspension-table');
    await expect(table).toContainText(reason);
    await table.getByRole('button').filter({ hasText: reason }).click();
    await expect(page).toHaveURL(new RegExp(`/account-info/suspensions/${id}$`));
    await expect(page.getByTestId('suspension-status')).toHaveText('검토 대기');

    // 확인 시작: 검토 중이 되고 확인자로 이름이 남는다.
    await page.getByRole('button', { name: '확인 시작' }).click();
    await page
      .getByRole('dialog', { name: '확인 시작' })
      .getByRole('button', { name: '확인 시작' })
      .click();
    await expect(page.getByTestId('suspension-status')).toHaveText('검토 중');
    await expect(page.getByTestId('suspension-reviewers')).toContainText('홍길동');

    // 조치 및 인수인계 사항을 쓰면 확정된다.
    await page.getByRole('button', { name: '조치 쓰고 확정' }).click();
    const handover = page.getByRole('dialog', { name: '조치 쓰고 확정' });
    await handover
      .getByRole('textbox', { name: '조치 및 인수인계 사항' })
      .fill(
        '운영팀이 01:00 에 가나은행 조회를 차단하고, 재개 연락을 받으면 풀고 상황실에 알립니다.',
      );
    await page.waitForTimeout(300);
    await shot(page, 'suspension-handover');
    await handover.getByRole('button', { name: '확정하기' }).click();
    await expect(page.getByTestId('suspension-status')).toHaveText('확정');
    await expect(page.getByTestId('suspension-handover')).toContainText('운영팀이 01:00 에');
    await expect(page.getByRole('button', { name: '조치 사항 고치기' })).toBeVisible();
    await shot(page, 'suspension-detail');

    // 변경 이력
    await page.getByRole('tab', { name: '변경 이력' }).click();
    await expect(page.getByText('조치 및 인수인계', { exact: false }).first()).toBeVisible();

    // 기관별 과거 이력: 가나은행을 골라 지난 일시중단을 본다.
    await page.getByRole('navigation').getByRole('link', { name: '일시중단', exact: true }).click();
    await page.getByRole('textbox', { name: '기관' }).click();
    await page.getByRole('option', { name: '가나은행' }).click();
    await expect(page).toHaveURL(/org=\d+/);
    await page
      .getByRole('group', { name: '시기' })
      .getByText(/지난 \d/)
      .click();
    const history = page.getByTestId('suspension-table');
    await expect(history).toContainText('재해복구 전환 훈련');
    await expect(history).not.toContainText('마바저축은행');
    await shot(page, 'suspension-organization-history');

    // 데모 도구: 전 영업일 취합을 운영·관제 채널로 보낸다.
    await page.getByRole('button', { name: '수동으로 보내기' }).click();
    const digest = page.getByRole('dialog', {
      name: '전 영업일 취합을 보냈습니다',
    });
    await expect(digest.getByTestId('digest-body')).toContainText('일시중단');
    await expect(digest).toContainText('~kftc-ops');
    await page.waitForTimeout(300);
    await shot(page, 'suspension-digest');
  });
});
