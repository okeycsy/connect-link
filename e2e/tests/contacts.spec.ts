import { expect, type Page, test } from '@playwright/test';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 기관 담당자(김민준)와 결제원 담당자(홍성민)로 로그인한다.
test.use({
  storageState: { cookies: [], origins: [] },
  timezoneId: 'Asia/Seoul',
});

/** 전체 화면 캡처. 위로 올린 뒤 찍어야 고정된 상단 바가 중간에 끼지 않는다. */
async function shot(page: Page, name: string) {
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.waitForTimeout(300);
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

async function loginAs(page: Page, button: string) {
  await page.goto('/login');
  await page.getByRole('button', { name: button }).click();
  await expect(page).toHaveURL(/\/home$/);
}

/** 데모 망 전환 값. 화면이 모든 요청에 X-Network-Zone 헤더로 싣는다. */
async function useZone(page: Page, zone: 'INTERNAL' | 'EXTERNAL') {
  await page.addInitScript((value) => {
    window.localStorage.setItem('demo.networkZone', value);
  }, zone);
}

/** 서울 기준 오늘에서 days 만큼 뒤의 날짜. */
function seoulDate(days: number) {
  const d = new Date(Date.now() + 9 * 3_600_000 + days * 86_400_000);
  return {
    year: d.getUTCFullYear(),
    month: d.getUTCMonth() + 1,
    day: d.getUTCDate(),
  };
}

/** 날짜 칸을 눌러 달력에서 고른다. 다음 달이면 달을 넘긴다. */
async function pickDate(page: Page, label: string, days: number) {
  const date = seoulDate(days);
  await page.getByRole('button', { name: label, exact: true }).click();
  const dropdown = page.getByRole('dialog').last();
  await expect(dropdown.getByRole('button', { name: /월 \d{4}$/ }).first()).toBeVisible();
  const dayButton = dropdown.getByRole('button', {
    name: `${date.day} ${date.month}월 ${date.year}`,
    exact: true,
  });
  if (!(await dayButton.isVisible())) {
    // 머리의 세 버튼은 이전 달, 달 이름, 다음 달 순서다.
    await dropdown.getByRole('button').nth(2).click();
  }
  await dayButton.click();
}

test.describe('가. 담당자 연락처', () => {
  test('기관 담당자는 로그인 때 6개월 경과 확인 창을 보고, 인터넷망에서 자기 기관 담당자를 고친다', async ({
    page,
  }) => {
    await useZone(page, 'EXTERNAL');
    await loginAs(page, '김민준 (참가기관 · 현업)로 로그인');

    // 최근 확인일이 6개월 지난 본인 행이 확인 창에 뜬다.
    const reconfirm = page.getByTestId('contact-reconfirm');
    await expect(reconfirm).toBeVisible();
    await expect(reconfirm).toContainText('김민준');
    await shot(page, 'contact-reconfirm');
    await reconfirm.getByRole('button', { name: '변동 없음' }).click();
    await expect(reconfirm.getByText('확인 완료')).toBeVisible();
    await reconfirm.getByRole('button', { name: '닫기' }).click();

    await page.getByRole('navigation').getByRole('link', { name: '연락처' }).click();
    await expect(page).toHaveURL(/\/account-info\/contacts$/);
    const table = page.getByTestId('contact-table');
    // 결제원과 자기 기관(KB국민은행) 담당자만 보인다.
    await expect(table.getByText('남궁민')).toBeVisible();
    await expect(table.getByText('홍성민')).toBeVisible();
    await expect(table.getByText('최유진')).toHaveCount(0);
    // 인터넷망에서는 검색·필터·엑셀이 없다.
    await expect(page.getByTestId('contact-filter-internal-only')).toBeVisible();
    await expect(page.getByRole('button', { name: '엑셀' })).toHaveCount(0);
    // 진행 중인 정기 변경 주간 안내와 현행화 완료 버튼
    const banner = page.getByTestId('campaign-banner');
    await expect(banner).toContainText('하반기 담당자 정기 변경');
    await expect(banner.getByRole('button', { name: '현행화 완료' })).toBeVisible();
    await shot(page, 'contact-list-member');

    // 같은 기관 담당자 상세: 이전 담당자 한 명까지, 휴대폰은 가려서.
    await table.getByRole('button', { name: 'KB국민은행 남궁민' }).click();
    const detail = page.getByTestId('contact-detail');
    await expect(detail).toBeVisible();
    const history = detail.getByTestId('change-history');
    await expect(history).toContainText('백도윤');
    await expect(history).toContainText('010-****-2299');
    await expect(history).toContainText('2299');
    await expect(page.getByRole('button', { name: '삭제' })).toHaveCount(0);
    await shot(page, 'contact-detail');

    // 고치면 최근 확인일과 확인한 사람이 바뀐다.
    await page.getByRole('button', { name: '수정', exact: true }).click();
    const form = page.getByRole('dialog', { name: '남궁민 담당자 수정' });
    await form.getByRole('textbox', { name: '내선번호' }).fill('2202');
    await form.getByRole('button', { name: '저장' }).click();
    await expect(form).toBeHidden();
    await expect(detail).toContainText('2202');
    await expect(detail).toContainText('김민준');
    await expect(detail.getByTestId('change-history')).toContainText('2201');

    // 결제원 담당자 행은 고치지 못한다.
    await page.keyboard.press('Escape');
    await table.getByRole('button', { name: '금융결제원 홍성민' }).click();
    await expect(page.getByTestId('contact-detail')).toBeVisible();
    await expect(page.getByRole('button', { name: '수정', exact: true })).toHaveCount(0);

    // 정기 변경 주간의 현행화 완료
    await page.keyboard.press('Escape');
    await banner.getByRole('button', { name: '현행화 완료' }).click();
    await expect(banner).toContainText('KB국민은행 현행화 완료');
  });

  test('결제원 담당자는 내부망에서 걸러 보고 엑셀을 받으며, 인터넷망에서는 고치지 못한다', async ({
    page,
  }) => {
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await page.goto('/account-info/contacts');
    const table = page.getByTestId('contact-table');
    await expect(table.getByText('최유진')).toBeVisible();
    await expect(page.getByTestId('campaign-banner')).toContainText('진행 현황 보기');

    // 확인 필요만 거르기
    await page.getByText(/^확인 필요만/).click();
    await expect(table.getByText('문하린')).toBeVisible();
    await expect(table.getByText('최유진')).toHaveCount(0);
    await shot(page, 'contact-list-kftc');

    // 검색
    await page.getByText(/^확인 필요만/).click();
    await page.getByRole('textbox', { name: '담당자 검색' }).fill('삼성');
    await expect(table.getByText('최유진')).toBeVisible();
    await expect(table.getByText('문하린')).toHaveCount(0);

    const download = page.waitForEvent('download');
    await page.getByRole('button', { name: '엑셀' }).click();
    // 헤드리스 Chromium 은 blob 내려받기 파일 이름을 "download" 로 주므로 내용(xlsx 는 zip, PK 로 시작)만 본다.
    const file = await (await download).createReadStream();
    const head: Buffer = await new Promise((resolve) => file.once('data', (c) => resolve(c as Buffer)));
    expect(head.subarray(0, 2).toString()).toBe('PK');

    // 인터넷망에서는 결제원 담당자의 수정·삭제 버튼이 없고 안내가 뜬다.
    await page.getByRole('button', { name: '데모 도구' }).click();
    await page.getByRole('menuitem', { name: /망 전환: 내부망 → 인터넷망/ }).click();
    await page.goto('/account-info/contacts');
    await expect(page.getByTestId('contact-filter-internal-only')).toBeVisible();
    await page
      .getByTestId('contact-table')
      .getByRole('button', { name: '삼성증권 최유진' })
      .click();
    await expect(page.getByTestId('contact-internal-only')).toBeVisible();
    await expect(page.getByRole('button', { name: '수정', exact: true })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '삭제' })).toHaveCount(0);
  });

  test('결제원 담당자는 정기 변경 주간의 진행률과 미확인 기관을 보고 주간을 등록·삭제한다', async ({
    page,
  }) => {
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await page.goto('/account-info/admin');
    await page
      .getByTestId('admin-menu')
      .getByRole('link', { name: /정기 변경 주간/ })
      .click();
    await expect(page).toHaveURL(/\/account-info\/admin\/update-campaigns$/);

    const progress = page.getByTestId('campaign-progress');
    await expect(progress).toContainText('하반기 담당자 정기 변경');
    await expect(progress.getByTestId('campaign-rate')).toHaveText(/\d+%/);
    const organizations = progress.getByTestId('campaign-organizations');
    await expect(organizations).toContainText('신한은행');
    await shot(page, 'update-campaign');

    await progress
      .getByRole('group', { name: '기관' })
      .getByText('미확인', { exact: true })
      .click();
    await expect(organizations.getByText('완료', { exact: true })).toHaveCount(0);
    await expect(organizations).toContainText('SBI저축은행');
    await progress
      .getByRole('group', { name: '기관' })
      .getByText('정보 변경', { exact: true })
      .click();
    await expect(organizations).toContainText('신협');

    // 예정 주간 등록 → 목록에 예정으로 보이고 지울 수 있다.
    await page.getByRole('button', { name: '주간 등록' }).click();
    const form = page.getByRole('dialog', { name: '정기 변경 주간 등록' });
    await form.getByRole('textbox', { name: '이름' }).fill('E2E 임시 정기 변경');
    await pickDate(page, '시작일', 12);
    await pickDate(page, '종료일', 14);
    await form.getByRole('button', { name: '등록' }).click();
    await expect(form).toBeHidden();
    await expect(progress).toContainText('E2E 임시 정기 변경');
    await expect(progress).toContainText('예정');

    await progress.getByRole('button', { name: '삭제' }).click();
    await page
      .getByRole('dialog', { name: '정기 변경 주간 삭제' })
      .getByRole('button', { name: '삭제' })
      .click();
    await expect(page.getByTestId('campaign-list')).not.toContainText('E2E 임시 정기 변경');
  });

  test('결제원 담당자는 미확인 기관에 리마인드 메일을 다시 보낸다', async ({ page }) => {
    await loginAs(page, '홍성민 (결제원 · 현업)로 로그인');
    await page.goto('/account-info/admin/update-campaigns');
    await expect(page.getByTestId('campaign-schedule')).toContainText('6개월 주기');
    await expect(page.getByTestId('campaign-schedule')).toContainText('3일마다 리마인드');

    // 기관 하나에 보내기
    const organizations = page.getByTestId('campaign-organizations');
    const cu = organizations.getByRole('row').filter({ hasText: '신협' });
    await cu.getByRole('button', { name: '신협 리마인드' }).click();
    const dialog = page.getByRole('dialog', { name: '리마인드 메일 보내기' });
    await expect(dialog).toContainText('신협 담당자에게');
    await dialog.getByRole('button', { name: '보내기' }).click();
    await expect(dialog).toBeHidden();
    await expect(cu).toContainText(/\d+회 · 최근/);

    // 미확인 기관 전부에 보내기
    await page.getByRole('button', { name: '미확인 기관 리마인드' }).click();
    await expect(dialog).toContainText('현행화 완료를 누르지 않은');
    await dialog.getByRole('button', { name: '보내기' }).click();
    await expect(page.getByText(/리마인드 메일을 보냈습니다/)).toBeVisible();
    await shot(page, 'update-campaign-reminder');
  });
});

