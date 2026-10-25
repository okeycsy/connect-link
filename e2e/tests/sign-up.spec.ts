import { expect, type Page, test } from '@playwright/test';

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다.
test.use({ storageState: { cookies: [], origins: [] } });

const PASSWORD = 'Welcome1!';

/** 데모 받은 메일함에 온 인증 코드를 읽는다. 프로토타입은 메일을 보내지 않고 발송함에 남긴다. */
async function codeFromMailbox(page: Page): Promise<string> {
  const mailbox = page.getByTestId('demo-mailbox');
  await expect(mailbox).toContainText('인증 코드:');
  const text = await mailbox.innerText();
  const code = /인증 코드: (\d{6})/.exec(text)?.[1];
  expect(code).toBeTruthy();
  return code!;
}

async function verifyEmail(page: Page, email: string) {
  await page.getByLabel('회사 이메일').fill(email);
  await page.getByRole('button', { name: '인증 코드 받기' }).click();
  const code = await codeFromMailbox(page);
  await page.getByLabel('인증 코드').fill(code);
  return code;
}

test.describe('회원가입', () => {
  test('로그인 화면에서 4단계로 가입을 신청하면 승인 대기가 되고 승인 전에는 로그인할 수 없다', async ({
    page,
  }) => {
    const email = `e2e.${Date.now()}@kb.example.com`;
    await page.goto('/login');
    await page.getByRole('link', { name: '회원가입', exact: true }).click();
    await expect(page).toHaveURL(/\/sign-up$/);
    await expect(page.getByRole('heading', { name: '회원가입' })).toBeVisible();

    // ① 회사 이메일 인증: 데모 메일함에서 코드를 읽어 넣는다.
    await verifyEmail(page, email);
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/sign-up-email.png', fullPage: true });
    await page.getByRole('button', { name: '확인', exact: true }).click();
    await expect(page.getByTestId('email-verified')).toBeVisible();
    await page.getByRole('button', { name: '다음' }).click();

    // ② 이메일 도메인이 같은 KB국민은행이 미리 골라져 있다.
    const step2 = page.getByTestId('sign-up-step-2');
    await expect(step2.getByRole('textbox', { name: '소속 기관' })).toHaveValue('KB국민은행');
    await expect(step2.getByText('회사 이메일 도메인과 맞는 기관입니다.')).toBeVisible();
    await step2.getByRole('checkbox', { name: '계좌통합관리' }).check();
    await step2.getByLabel('계좌통합관리 세부업무').fill('어카운트인포 앱');
    await step2.getByRole('checkbox', { name: 'CMS' }).check();
    await page.waitForTimeout(300);
    await page.screenshot({ path: 'screenshots/sign-up-organization.png', fullPage: true });
    await page.getByRole('button', { name: '다음' }).click();

    // ③ 전산이면 세부 직무를 고른다.
    const step3 = page.getByTestId('sign-up-step-3');
    await step3.getByRole('radio', { name: '전산' }).check();
    await step3.getByRole('checkbox', { name: '운영' }).check();
    await step3.getByRole('checkbox', { name: '상황실' }).check();
    await page.getByRole('button', { name: '다음' }).click();

    // ④ 담당자 정보와 비밀번호. 위에 앞 단계 요약이 있다.
    const step4 = page.getByTestId('sign-up-step-4');
    await expect(step4.getByTestId('sign-up-summary')).toContainText('전산(운영, 상황실)');
    await step4.getByLabel('이름').fill('가입자');
    await step4.getByLabel('직급').fill('대리');
    await step4.getByLabel('부서').fill('디지털금융부');
    await step4.getByRole('textbox', { name: '팀', exact: true }).fill('플랫폼팀');
    await step4.getByLabel('내선번호').fill('1234');
    await step4.getByLabel('휴대폰번호').fill('010-0000-7777');
    // 서버 규칙에 맞지 않는 비밀번호는 서버 메시지를 그 칸 아래에 보여 준다.
    await step4
      .getByLabel(/^비밀번호/)
      .first()
      .fill('password');
    await step4.getByLabel('비밀번호 확인').fill('password');
    await page.getByRole('button', { name: '가입 신청' }).click();
    await expect(
      step4.getByText('비밀번호는 영문, 숫자, 특수문자를 모두 넣어 8~64자로 정해 주세요.'),
    ).toBeVisible();
    await step4
      .getByLabel(/^비밀번호/)
      .first()
      .fill(PASSWORD);
    await step4.getByLabel('비밀번호 확인').fill(PASSWORD);
    await page.screenshot({ path: 'screenshots/sign-up-profile.png', fullPage: true });
    await page.getByRole('button', { name: '가입 신청' }).click();

    // 승인 대기
    await expect(page).toHaveURL(/\/sign-up\/pending$/);
    const applied = page.getByTestId('sign-up-applied');
    await expect(applied).toContainText(email);
    await expect(applied).toContainText('KB국민은행 · 참가기관');
    await expect(applied).toContainText('계좌통합관리');
    await expect(applied).toContainText('CMS');
    await expect(applied).toContainText('전산(운영, 상황실)');
    await page.screenshot({ path: 'screenshots/sign-up-pending.png', fullPage: true });

    // 승인 전에는 로그인하면 승인 대기 안내가 뜬다.
    await page.getByRole('link', { name: '로그인 화면으로' }).click();
    await page.getByLabel('회사 이메일').fill(email);
    await page.getByLabel('비밀번호').fill(PASSWORD);
    await page.getByRole('button', { name: '로그인', exact: true }).click();
    await expect(page.getByTestId('login-error')).toContainText('가입 승인 전입니다.');
  });

  test('이미 가입한 이메일은 코드를 보내지 않고, 도메인과 다른 기관을 고르면 안내가 뜬다', async ({
    page,
  }) => {
    await page.goto('/sign-up');
    await page.getByLabel('회사 이메일').fill('kim.minjun@kb.example.com');
    await page.getByRole('button', { name: '인증 코드 받기' }).click();
    await expect(page.getByText('이미 가입했거나 가입을 신청한 이메일입니다.')).toBeVisible();

    await page.getByLabel('회사 이메일').fill(`e2e.${Date.now()}@other.example.com`);
    await page.getByRole('button', { name: '인증 코드 받기' }).click();
    const code = await codeFromMailbox(page);
    // 틀린 코드는 남은 횟수를 알린다.
    await page.getByLabel('인증 코드').fill(code === '000000' ? '111111' : '000000');
    await page.getByRole('button', { name: '확인', exact: true }).click();
    await expect(page.getByText(/인증 코드가 맞지 않습니다\. 4번 더 틀리면/)).toBeVisible();
    await page.getByLabel('인증 코드').fill(code);
    await page.getByRole('button', { name: '확인', exact: true }).click();
    await page.getByRole('button', { name: '다음' }).click();

    // 도메인이 맞는 기관이 없어 미리 고르지 않는다. 고르면 승인 때 확인한다는 안내가 뜬다.
    const step2 = page.getByTestId('sign-up-step-2');
    const select = step2.getByRole('textbox', { name: '소속 기관' });
    await expect(select).toHaveValue('');
    await select.click();
    await page.getByRole('option', { name: '신한은행' }).click();
    await expect(step2.getByTestId('domain-mismatch')).toBeVisible();
    await expect(step2.getByRole('checkbox', { name: '대출이동' })).toBeDisabled();
  });
});
