import { type APIRequestContext, expect, type Page, test } from '@playwright/test';

import { csrf, mailbox, skipContactReconfirm } from './support/account';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 결제원 담당자(홍성민)와 기관 담당자(김민준)로 로그인한다.
test.use({ storageState: { cookies: [], origins: [] } });

const SEEDED = '계좌통합관리 수수료 체계 개편 의견 요청';
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

async function apiLogin(request: APIRequestContext, email: string) {
  const login = await request.post('/api/v1/auth/login', {
    data: { email, password: DEMO_PASSWORD },
    headers: await csrf(request),
  });
  expect(login.ok()).toBeTruthy();
}

/** 홍성민으로 KB국민은행(김민준)만 대상인 계좌통합관리 의견 요청을 만들어 회신중으로 연다. 객관식(필수)·주관식 질문이 하나씩 있다. */
async function openRequestForGana(request: APIRequestContext, title: string): Promise<number> {
  await apiLogin(request, 'hong.sungmin@kftc.example.com');
  const candidates = (await (
    await request.get('/api/v1/opinion-requests/recipient-candidates?business=account-info')
  ).json()) as {
    organizationId: number;
    organizationCode: string;
    members: { contactId: number; email: string }[];
  }[];
  const gana = candidates.find((c) => c.organizationCode === '004')!;
  const kim = gana.members.find((m) => m.email === 'kim.minjun@kb.example.com')!;
  const created = await request.post('/api/v1/opinion-requests', {
    data: {
      business: 'ACCOUNT_INFO',
      title,
      content: '변경안에 대한 의견을 주세요.',
      dueDate: new Date(Date.now() + 7 * 86_400_000).toISOString().slice(0, 10),
      questions: [
        {
          type: 'SINGLE_CHOICE',
          title: '전환 시점으로 어느 때가 좋습니까?',
          required: true,
          options: ['다음 달', '다음 분기'],
        },
        { type: 'TEXT', title: '준비에 필요한 기간을 적어 주세요.', required: false },
      ],
      targets: [
        {
          organizationId: gana.organizationId,
          recipientEmails: [kim.email],
        },
      ],
    },
    headers: await csrf(request),
  });
  expect(created.ok(), await created.text()).toBeTruthy();
  const { id } = (await created.json()) as { id: number };
  const opened = await request.post(`/api/v1/opinion-requests/${id}/opening`, {
    headers: await csrf(request),
  });
  expect(opened.ok()).toBeTruthy();
  return id;
}

test.describe('나. 의견 요청', () => {
  test.beforeEach(async ({ page }) => skipContactReconfirm(page));

  test('결제원 담당자가 회신 현황을 보고 리마인드·엑셀을 쓰고, 새 요청을 만들어 회신을 요청한다', async ({
    page,
    request,
  }) => {
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await page
      .getByRole('navigation')
      .getByRole('link', { name: '의견 요청', exact: true })
      .click();
    await expect(page).toHaveURL(/\/account-info\/opinions$/);
    await expect(page.getByRole('heading', { name: '의견 요청' })).toBeVisible();
    const seededRow = page.getByTestId('opinion-table').getByRole('button', { name: SEEDED });
    await expect(seededRow).toContainText('4/6곳');
    await shot(page, 'opinion-list');

    // 회신 현황: 제출 기관 수·회신율·유형별 기관
    await seededRow.click();
    await expect(page.getByRole('heading', { name: SEEDED })).toBeVisible();
    await shot(page, 'opinion-detail');
    await page.getByRole('tab', { name: '회신 현황' }).click();
    await expect(page.getByTestId('reply-rate')).toHaveText('67%');
    await expect(page.getByTestId('not-replied-names')).toContainText('KB국민은행, 신협');
    // 질문별 답: 선택지마다 고른 기관 수와 이름, 주관식 답.
    const questionSummary = page.getByTestId('question-summary');
    await expect(questionSummary).toContainText('개편안을 언제부터 적용하는 것이 좋습니까?');
    await expect(questionSummary).toContainText('SBI저축은행, 저축은행중앙회');
    await expect(questionSummary).toContainText('약 12만 건');
    await shot(page, 'opinion-replies');

    // 미회신만 걸러 보기
    await page
      .getByRole('group', { name: '회신' })
      .getByText(/미회신 \d/)
      .click();
    const statusTable = page.getByTestId('reply-status-table');
    await expect(statusTable.getByRole('button', { name: 'KB국민은행 회신' })).toBeVisible();
    await expect(statusTable.getByRole('button', { name: '신한은행 회신' })).toHaveCount(0);

    // 수정 요청한 기관은 패널에서 사유를 본다.
    await page
      .getByRole('group', { name: '회신' })
      .getByText(/전체 \d/)
      .click();
    await statusTable.getByRole('button', { name: '삼성증권 회신' }).click();
    await expect(page.getByTestId('revision-request')).toBeVisible();
    await page.waitForTimeout(300);
    await shot(page, 'opinion-reply-drawer');
    await page.keyboard.press('Escape');

    // 리마인드: 미회신 기관 담당자에게 메일이 간다.
    await page.getByRole('button', { name: '미회신 기관 리마인드' }).click();
    const remind = page.getByRole('dialog', { name: '리마인드 메일 보내기' });
    await expect(remind).toContainText('KB국민은행');
    await page.waitForTimeout(300);
    await shot(page, 'opinion-remind');
    await remind.getByRole('button', { name: /곳에 보내기$/ }).click();
    await expect(page.getByTestId('remind-sent')).toContainText('리마인드 메일을 보냈습니다');
    const mails = await mailbox(request, 'kim.minjun@kb.example.com');
    expect(mails.some((m) => m.subject.includes(`회신을 기다립니다: ${SEEDED}`))).toBeTruthy();
    await page.getByRole('tab', { name: '리마인드 이력' }).click();
    await expect(page.getByTestId('reminder-history')).toContainText('홍성민');

    // 취합표와 엑셀(내부망)
    await page.getByRole('tab', { name: '취합표' }).click();
    await expect(page.getByTestId('collation-table')).toContainText('신한은행');
    await expect(page.getByTestId('collation-table')).toContainText('수수료 정산, 고객 안내');
    await shot(page, 'opinion-collation');
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: '엑셀 다운로드' }).click(),
    ]);
    // 화면은 응답 헤더의 이름으로 저장한다(헤드리스 크롬은 blob 저장 이름을 넘기지 않아 내용과 헤더를 따로 본다).
    const saved = await download.createReadStream();
    const head: Buffer = await new Promise((resolve) => saved.once('data', resolve));
    expect(head.subarray(0, 2).toString()).toBe('PK');
    const excel = await page.request.get(
      page
        .url()
        .replace(/\?.*$/, '')
        .replace('/account-info/opinions/', '/api/v1/opinion-requests/') + '/collation/excel',
    );
    expect(decodeURIComponent(excel.headers()['content-disposition'])).toMatch(
      /filename\*=UTF-8''의견취합_.+\.xlsx$/,
    );

    // 새 요청: 질문(객관식·주관식)과 첨부자료를 넣고, 세부업무 칩·한 명씩 체크·업권 칩으로 담당자를 고른다.
    const title = `연동 일정 의견 요청 ${Date.now().toString(36).slice(-5)}`;
    await page
      .getByRole('navigation')
      .getByRole('link', { name: '의견 요청', exact: true })
      .click();
    await page.getByRole('link', { name: '새 의견 요청' }).click();
    await expect(page).toHaveURL(/\/account-info\/opinions\/new$/);
    await page.getByLabel('요청명').fill(title);
    await page.getByLabel('요청 목적·내용').fill('다음 분기 연동 일정 초안에 대한 의견을 주세요.');
    await page
      .getByRole('textbox', { name: '1번 질문', exact: true })
      .fill('연동 시작 시기를 골라 주세요.');
    await page.getByRole('textbox', { name: '1번 질문 선택지 1', exact: true }).fill('1분기');
    await page.getByRole('textbox', { name: '1번 질문 선택지 2', exact: true }).fill('2분기');
    await page.getByRole('button', { name: '선택지 추가' }).click();
    await page.getByRole('textbox', { name: '1번 질문 선택지 3', exact: true }).fill('하반기');
    await page.getByRole('button', { name: '질문 추가' }).click();
    await page
      .getByRole('textbox', { name: '2번 질문', exact: true })
      .fill('연동 담당 부서를 적어 주세요.');
    await page.getByRole('textbox', { name: '2번 질문 유형' }).click();
    await page.getByRole('option', { name: '주관식' }).click();
    await expect(page.getByTestId('question-card').nth(1)).toContainText('기관이 글로 답합니다.');
    await page
      .getByTestId('pending-attachments')
      .locator('input[type=file]')
      .setInputFiles({
        name: '연동일정안.pdf',
        mimeType: 'application/pdf',
        buffer: Buffer.from('%PDF-1.4 sample'),
      });
    await expect(page.getByTestId('pending-attachments')).toContainText('연동일정안.pdf');
    await page
      .getByRole('group', { name: '세부업무로 고르기' })
      .getByText('오픈뱅킹 안심차단', { exact: true })
      .click();
    await expect(page.getByRole('checkbox', { name: '삼성증권 최유진' })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: '저축은행중앙회 윤서연' })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: '신한은행 박지훈' })).not.toBeChecked();
    await page.getByRole('checkbox', { name: 'KB국민은행 김민준' }).check();
    await expect(page.getByTestId('target-summary')).toHaveText('대상 3곳 · 회신 담당자 3명');
    // 업권을 고르면 그 업권 기관이 담당자 연락처와 함께 모두 대상이 된다(계정이 없는 담당자도 고른다).
    await page
      .getByRole('group', { name: '업권으로 고르기' })
      .getByText(/^서민금융기관 \d+곳$/)
      .click();
    await expect(page.getByRole('checkbox', { name: 'SBI저축은행 대상' })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: '신협 대상' })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: '신협 정현우' })).toBeChecked();
    await expect(page.getByRole('checkbox', { name: 'SBI저축은행 문하린' })).toBeChecked();
    await expect(page.getByTestId('target-summary')).toHaveText('대상 5곳 · 회신 담당자 6명');
    await shot(page, 'opinion-editor');
    await page.getByRole('button', { name: '저장', exact: true }).click();

    await expect(page.getByRole('heading', { name: title })).toBeVisible();
    await expect(page.getByText('작성중', { exact: true })).toBeVisible();
    await expect(page.getByTestId('question-list')).toContainText(
      '1. 연동 시작 시기를 골라 주세요.',
    );
    await expect(page.getByTestId('question-list')).toContainText('1분기 / 2분기 / 하반기');
    await expect(page.getByTestId('attachment-panel').first()).toContainText('연동일정안.pdf');
    await page.getByRole('button', { name: '회신 요청 시작' }).click();
    await page
      .getByRole('dialog', { name: '회신 요청 시작' })
      .getByRole('button', { name: '시작하기' })
      .click();
    await expect(page.getByText('회신중', { exact: true })).toBeVisible();
    const opened = await mailbox(request, 'kim.minjun@kb.example.com');
    expect(opened.some((m) => m.subject.includes(`의견 요청: ${title}`))).toBeTruthy();
  });

  test('기관 담당자가 임시저장·최종제출하고, 수정 요청이 승인되면 다시 고친다', async ({
    page,
    browser,
  }) => {
    const title = `회신 흐름 확인 ${Date.now().toString(36).slice(-5)}`;
    const hong = await browser.newContext({
      storageState: { cookies: [], origins: [] },
    });
    const id = await openRequestForGana(hong.request, title);

    await loginAs(page, '김민준 (참가기관 · 현업)로 로그인');
    await page
      .getByRole('navigation')
      .getByRole('link', { name: '의견 요청', exact: true })
      .click();
    const row = page.getByTestId('opinion-table').getByRole('button', { name: title });
    await expect(row).toContainText('미작성');
    await row.click();
    await expect(page).toHaveURL(new RegExp(`/account-info/opinions/${id}$`));
    const reply = page.getByTestId('my-reply');

    // 의견 유형·세부의견 칸은 없고 결제원 담당자가 넣은 질문만 있다.
    await expect(reply.getByRole('radio', { name: '동의', exact: true })).toHaveCount(0);
    await expect(reply.getByLabel('세부의견')).toHaveCount(0);

    // 필수 질문에 답하지 않으면 그 질문 아래에 서버 메시지가 뜬다.
    await reply.getByRole('button', { name: '최종제출' }).click();
    await expect(reply).toContainText('필수 질문입니다. 답해 주세요.');
    await reply.getByRole('radio', { name: '다음 분기' }).check();
    await reply
      .getByRole('textbox', { name: '2. 준비에 필요한 기간을 적어 주세요.' })
      .fill('약 6주');
    await reply.getByRole('button', { name: '임시저장' }).click();
    await expect(page.getByTestId('reply-notice')).toContainText('임시저장했습니다');
    await expect(reply).toContainText('근거자료 올리기');
    await shot(page, 'opinion-my-reply-draft');

    await reply.getByRole('button', { name: '최종제출' }).click();
    await expect(page.getByTestId('submitted-answers')).toContainText('다음 분기');
    await expect(page.getByTestId('submitted-answers')).toContainText('약 6주');
    await expect(reply).toContainText('제출');

    // 제출한 뒤 고치려면 수정 요청을 한다.
    await reply.getByRole('button', { name: '수정 요청' }).click();
    const dialog = page.getByRole('dialog', { name: '회신 수정 요청' });
    await dialog.getByLabel('수정 사유').fill('일정 근거를 덧붙이려고 합니다.');
    await dialog.getByRole('button', { name: '요청 보내기' }).click();
    await expect(reply).toContainText('수정 요청을 보냈습니다');
    await expect(dialog).toHaveCount(0);
    await shot(page, 'opinion-my-reply-submitted');

    // 결제원 담당자가 승인하면 수정 중이 되고 제출한 내용에서 이어 고친다.
    const collation = (await (
      await hong.request.get(`/api/v1/opinion-requests/${id}/collation`)
    ).json()) as { rows: { replyId: number }[] };
    const approved = await hong.request.post(
      `/api/v1/opinion-requests/${id}/replies/${collation.rows[0].replyId}/revision-approval`,
      { headers: await csrf(hong.request) },
    );
    expect(approved.ok()).toBeTruthy();
    await hong.close();

    await page.reload();
    await expect(reply).toContainText('수정 요청이 승인되었습니다');
    await expect(reply.getByRole('radio', { name: '다음 분기' })).toBeChecked();
    await reply.getByRole('radio', { name: '다음 달' }).check();
    await reply.getByRole('button', { name: '최종제출' }).click();
    await expect(page.getByTestId('submitted-answers')).toContainText('다음 달');
  });
});
