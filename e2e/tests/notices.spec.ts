import { expect, type Page, test } from "@playwright/test";

// 공지·자료실: 결제원 담당자(홍성민)가 업무 공지를 올리고, 참가기관 담당자(김민준)가 공통 공지·규약과 함께 본다.
test.use({
  storageState: { cookies: [], origins: [] },
  timezoneId: "Asia/Seoul",
});

async function shot(page: Page, name: string) {
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.waitForTimeout(300);
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

async function loginAs(page: Page, button: string) {
  await page.goto("/login");
  await page.getByRole("button", { name: button }).click();
  await expect(page).toHaveURL(/\/home$/);
}

test("결제원 담당자가 공지를 올리고 기관 담당자가 본다", async ({
  browser,
}) => {
  const title = `E2E 공지 ${Date.now()}`;

  const kftc = await browser.newPage();
  await loginAs(kftc, "홍성민 (결제원 · 현업)로 로그인");
  await kftc.getByRole("link", { name: "공지·자료실" }).first().click();
  await expect(kftc).toHaveURL(/\/account-info\/notices$/);
  await kftc.getByRole("button", { name: "글 쓰기" }).click();
  await kftc.getByLabel("제목").fill(title);
  await kftc.getByLabel("내용").fill("E2E 로 올린 공지입니다.");
  await kftc.getByRole("button", { name: "올리기" }).click();
  await expect(kftc.getByTestId("notice-list")).toContainText(title);
  await shot(kftc, "notices-kftc");

  const bank = await browser.newPage();
  await loginAs(bank, "김민준 (참가기관 · 현업)로 로그인");
  await bank.goto("/account-info/notices");
  // 김민준은 6개월 넘게 확인하지 않은 담당자라 로그인 뒤 확인 창이 뜬다. 이 시나리오와 상관없어 닫는다.
  await expect(bank.getByTestId("contact-reconfirm")).toBeVisible();
  await bank.keyboard.press("Escape");
  await expect(bank.getByTestId("notice-list")).toContainText(
    "연결이 이용 안내",
  );
  await expect(bank.getByTestId("notice-list")).toContainText(
    "계좌통합관리업무 규약(예시)",
  );
  await expect(bank.getByRole("button", { name: "글 쓰기" })).toHaveCount(0);
  await bank.getByText(title).click();
  await expect(bank.getByRole("dialog")).toContainText(
    "E2E 로 올린 공지입니다.",
  );
  await shot(bank, "notices-detail");

  // 회의 개최 안내는 참석 회신 요청을 이어 두어, 대상 기관 담당자는 글에서 바로 회신하러 간다.
  await bank.keyboard.press("Escape");
  await bank.getByText("계좌통합관리업무 전문 변경 회의 개최 안내").click();
  await expect(bank.getByTestId("notice-opinion")).toContainText(
    "계좌통합관리업무 전문 변경 회의 참석 회신",
  );
  await bank.getByRole("link", { name: "회신하기" }).click();
  await expect(bank).toHaveURL(/\/account-info\/opinions\/\d+$/);
  await expect(bank.getByText("회의에 참석하십니까?")).toBeVisible();
});
