import { expect, test } from "@playwright/test";

// 로그인 없이 보는 화면이라 빈 브라우저로 시작한다.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe("개인정보 처리방침", () => {
  test("로그인 화면 아래 링크로 로그인 없이 처리방침을 본다", async ({ page }) => {
    await page.goto("/login");
    const link = page.getByTestId("login-privacy-link");
    await expect(link).toBeVisible();
    await link.click();

    await expect(page).toHaveURL(/\/privacy$/);
    await expect(
      page.getByRole("heading", { name: "개인정보처리방침", level: 1 }),
    ).toBeVisible();
    const policy = page.getByTestId("privacy-policy");
    await expect(policy).toContainText("제1조(개인정보의 처리 목적)");
    await expect(policy).toContainText("제17조(개인정보 처리방침의 변경에 관한 사항)");
    await expect(policy).toContainText("제3자에게 제공하지 않습니다");
    // 관리부서 연락처는 서버 설정(demo 는 가상 값)에서 읽는다.
    const officer = page.getByTestId("privacy-officer");
    await expect(officer).toContainText("정보보호최고책임자(CISO)");
    await expect(officer).toContainText("연결이 운영팀");
    await expect(officer).toContainText("help@connect-link.example.com");
    await page.screenshot({ path: "screenshots/privacy-policy.png", fullPage: true });
  });

  test("휴대폰 폭에서도 옆으로 밀리지 않는다", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/privacy");
    await expect(page.getByTestId("privacy-policy")).toBeVisible();
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);
    await page.screenshot({ path: "screenshots/privacy-policy-mobile.png", fullPage: true });
  });
});
