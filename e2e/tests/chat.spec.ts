import { expect, type Page, test } from '@playwright/test';

import { skipContactReconfirm } from './support/account';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 결제원 담당자(홍성민)와 기관 담당자(김민준)로 로그인한다.
test.use({
  storageState: { cookies: [], origins: [] },
  timezoneId: 'Asia/Seoul',
});

/** 화면 캡처. 대화 칸은 자체 스크롤이라 보이는 화면만 찍는다. */
async function shot(page: Page, name: string) {
  await page.waitForTimeout(300);
  await page.screenshot({ path: `screenshots/${name}.png` });
}

async function loginAs(page: Page, button: string) {
  await page.goto('/login');
  await page.getByRole('button', { name: button }).click();
  await expect(page).toHaveURL(/\/home$/);
}

async function openChat(page: Page) {
  await page.getByRole('navigation').getByRole('link', { name: '소통 채널' }).click();
  await expect(page).toHaveURL(/\/account-info\/chat$/);
  await expect(page.getByRole('heading', { name: '소통 채널' })).toBeVisible();
}

const rooms = (page: Page) => page.getByRole('complementary', { name: '채팅방 목록' });

test.describe('마. 소통 채널', () => {
  test.describe.configure({ mode: 'serial' });
  test.beforeEach(async ({ page }) => skipContactReconfirm(page));

  test('결제원 담당자가 결제원 멘션을 읽고, @기관 을 불러 파일과 함께 보낸다', async ({ page }) => {
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await openChat(page);

    // 업무 단체방에 결제원을 부른 읽지 않은 메시지가 있다.
    const groupRoom = rooms(page).getByRole('link', {
      name: /계좌통합관리 단체방/,
    });
    await expect(groupRoom.getByLabel('내 기관 멘션')).toBeVisible();
    await expect(groupRoom.getByLabel('읽지 않은 메시지')).toHaveText('1');
    await expect(rooms(page)).toContainText('오픈뱅킹 안심차단 전문 테스트');
    await groupRoom.click();

    const messages = page.getByTestId('chat-messages');
    await expect(messages).toContainText('삼성증권은 오늘 18시 이후에');
    await expect(messages.getByText('@결제원', { exact: true })).toBeVisible();
    // 김민준이 확인한 공지에는 기관별 확인이 보인다.
    await expect(messages.getByTestId('chat-ack').first()).toHaveText('KB국민은행 1');
    // 맨 아래까지 보았으니 읽음이 되어 배지가 사라진다.
    await expect(groupRoom.getByLabel('읽지 않은 메시지')).toHaveCount(0);

    // @ 를 치면 이 방에서 부를 수 있는 기관이 뜬다.
    const input = page.getByRole('textbox', { name: '메시지 입력' });
    await input.click();
    await input.pressSequentially('@KB');
    await page.getByRole('option', { name: '@KB국민은행' }).click();
    await input.pressSequentially('E2E 점검 공지입니다. 첨부 확인 부탁드립니다.');
    await page.locator('input[type=file]').setInputFiles({
      name: 'e2e-점검-일정.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('점검 일정(샘플)'),
    });
    await expect(page.getByText('e2e-점검-일정.txt')).toBeVisible();
    await shot(page, 'chat-compose');
    await page.getByRole('button', { name: '보내기' }).click();

    const mine = messages.getByTestId('chat-message').last();
    await expect(mine).toContainText('E2E 점검 공지입니다.');
    await expect(mine.getByText('@KB국민은행', { exact: true })).toBeVisible();
    await expect(mine.getByRole('link', { name: 'e2e-점검-일정.txt' })).toBeVisible();
    await expect(input).toHaveValue('');
    await shot(page, 'chat-room-kftc');
  });

  test('결제원 담당자가 초대방을 만들고 업무 밖 결제원 사람을 초대한다', async ({ page }) => {
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await openChat(page);

    await page.getByRole('button', { name: '초대방 만들기' }).click();
    await page.getByLabel('방 이름').fill('E2E 장애 대응 방');
    await page.getByRole('button', { name: '만들기', exact: true }).click();

    // 만들면 바로 초대 창이 열린다. 이름 검색 없이 업무·소속·직무로 거르고 기관 묶음째 고른다.
    const dialog = page.getByRole('dialog', { name: '사람 초대' });
    await expect(dialog).toBeVisible();
    await expect(dialog.getByRole('textbox', { name: '담당 업무' })).toHaveValue('계좌통합관리');
    await dialog
      .getByRole('group', { name: '소속' })
      .getByText('참가기관', { exact: true })
      .click();
    await expect(dialog.getByText(/^결제원 \d+명$/)).toHaveCount(0);
    await dialog.getByRole('checkbox', { name: /^삼성증권 \d+명$/ }).check();
    await expect(dialog.getByRole('checkbox', { name: /최유진/ })).toBeChecked();
    await shot(page, 'chat-invite');

    // 업무 밖 결제원 사람(CMS 박하늘)도 업무를 바꿔 고른다. 앞서 고른 사람은 남는다.
    await dialog.getByRole('textbox', { name: '담당 업무' }).click();
    await page.getByRole('option', { name: 'CMS' }).click();
    await dialog.getByRole('group', { name: '소속' }).getByText('결제원', { exact: true }).click();
    await dialog.getByRole('group', { name: '직무' }).getByText('현업', { exact: true }).click();
    await dialog.getByRole('checkbox', { name: /박하늘/ }).check();
    await shot(page, 'chat-invite-other-business');
    await dialog.getByRole('button', { name: /명 초대$/ }).click();
    await expect(dialog).toBeHidden();

    await expect(page.getByRole('heading', { name: 'E2E 장애 대응 방' })).toBeVisible();
    await expect(page.getByRole('button', { name: '3' })).toBeVisible();
    await expect(rooms(page)).toContainText('E2E 장애 대응 방');
  });

  test('기관 담당자는 우리 기관을 부른 메시지에 확인을 누른다', async ({ page }) => {
    await loginAs(page, '김민준 (참가기관 · 현업)로 로그인');
    await openChat(page);

    // 초대받지 않은 방은 보이지 않고, 초대방을 만드는 버튼도 없다.
    await expect(rooms(page)).not.toContainText('오픈뱅킹 안심차단 전문 테스트');
    await expect(rooms(page)).not.toContainText('E2E 장애 대응 방');
    await expect(page.getByRole('button', { name: '초대방 만들기' })).toHaveCount(0);

    const groupRoom = rooms(page).getByRole('link', {
      name: /계좌통합관리 단체방/,
    });
    await expect(groupRoom.getByLabel('내 기관 멘션')).toBeVisible();
    await groupRoom.click();

    const message = page
      .getByTestId('chat-message')
      .filter({ hasText: 'E2E 점검 공지입니다.' })
      .last();
    await expect(message.getByRole('link', { name: 'e2e-점검-일정.txt' })).toBeVisible();
    await message.getByRole('button', { name: '확인', exact: true }).click();
    await expect(message.getByTestId('chat-ack')).toHaveText('KB국민은행 1');
    await expect(message.getByRole('button', { name: '확인 취소' })).toBeVisible();
    await shot(page, 'chat-room-organization');
  });

  test('휴대폰 폭에서는 방 목록과 대화를 한 쪽씩 본다', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await loginAs(page, '김민준 (참가기관 · 현업)로 로그인');
    await page.goto('/account-info/chat');
    await expect(rooms(page)).toBeVisible();
    await rooms(page)
      .getByRole('link', { name: /계좌통합관리 단체방/ })
      .click();
    await expect(rooms(page)).toBeHidden();
    await expect(page.getByRole('textbox', { name: '메시지 입력' })).toBeVisible();
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);
    await shot(page, 'mobile-chat-room');

    await page.getByRole('link', { name: '방 목록으로' }).click();
    await expect(rooms(page)).toBeVisible();
  });
});
