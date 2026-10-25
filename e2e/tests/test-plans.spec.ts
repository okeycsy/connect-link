import {
  type APIRequestContext,
  expect,
  type Page,
  test,
} from "@playwright/test";

import { csrf, mailbox, skipContactReconfirm } from "./support/account";

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 결제원 담당자(홍성민)와 기관 담당자(김민준)로 로그인한다.
test.use({
  storageState: { cookies: [], origins: [] },
  timezoneId: "Asia/Seoul",
});

const DEMO_PASSWORD = "demo1234!";

/** 전체 화면 캡처. 위로 올린 뒤 찍어야 고정된 상단 바가 중간에 끼지 않는다. */
async function shot(page: Page, name: string) {
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: `screenshots/${name}.png`, fullPage: true });
}

async function loginAs(page: Page, button: string) {
  await page.context().clearCookies();
  await page.goto("/login");
  await page.getByRole("button", { name: button }).click();
  await expect(page).toHaveURL(/\/home$/);
}

/** 서울 기준 오늘에서 days 만큼 뒤의 날짜(YYYY-MM-DD). */
function seoulDate(days: number) {
  return new Date(Date.now() + 9 * 3_600_000 + days * 86_400_000)
    .toISOString()
    .slice(0, 10);
}

/** 홍성민으로 KB국민은행 대상 계좌통합관리 기관 테스트를 API 로 만든다. request 의 세션은 홍성민으로 바뀐다. */
async function createAsHong(
  request: APIRequestContext,
  title: string,
  testDataRequest: {
    dueDate: string;
    instruction: string | null;
  } | null = null,
): Promise<number> {
  const login = await request.post("/api/v1/auth/login", {
    data: { email: "hong.sungmin@kftc.example.com", password: DEMO_PASSWORD },
    headers: await csrf(request),
  });
  expect(login.ok()).toBeTruthy();
  const organizations = (await (
    await request.get("/api/v1/test-plans/organizations?business=account-info")
  ).json()) as { id: number; code: string }[];
  const created = await request.post("/api/v1/test-plans", {
    data: {
      business: "ACCOUNT_INFO",
      subBusiness: "계좌정보 조회·해지",
      title,
      description: "해지 요청 전문의 새 사유코드를 확인합니다.",
      startDate: seoulDate(0),
      endDate: seoulDate(7),
      items: [
        {
          title: "해지 요청 전문 처리 확인",
          description: "사유코드 07~09 를 처리하는지 확인합니다.",
        },
        { title: "해지 결과 통보 전문 확인", description: null },
      ],
      organizationIds: [organizations.find((o) => o.code === "004")!.id],
      testDataRequest,
    },
    headers: await csrf(request),
  });
  expect(created.ok(), await created.text()).toBeTruthy();
  return ((await created.json()) as { id: number }).id;
}

test.describe("다. 기관 테스트", () => {
  test.beforeEach(async ({ page }) => skipContactReconfirm(page));

  test("결제원 담당자가 기관 테스트를 만들고 결과 등록 알림 대상을 정하면 대상 기관에 메일이 간다", async ({
    page,
    request,
  }) => {
    await loginAs(page, "홍성민 (결제원 · 현업)로 로그인");
    await page
      .getByRole("navigation")
      .getByRole("link", { name: "기관 테스트", exact: true })
      .click();
    await expect(page).toHaveURL(/\/account-info\/tests$/);
    await expect(
      page.getByRole("heading", { name: "기관 테스트" }),
    ).toBeVisible();
    const list = page.getByTestId("test-plan-table");
    await expect(list).toContainText("계좌정보 통합조회 전문 개선 2차");
    await expect(list).toContainText("기간 지남");
    await shot(page, "test-plan-list");

    await page.getByRole("link", { name: "기관 테스트 만들기" }).click();
    await expect(
      page.getByRole("heading", { name: "기관 테스트 만들기" }),
    ).toBeVisible();
    const title = `E2E 해지 전문 개선 ${Date.now()}`;
    await page
      .getByRole("textbox", { name: "단위업무" })
      .fill("계좌정보 조회·해지");
    await page.getByRole("textbox", { name: "개발 건 이름" }).fill(title);
    await page
      .getByRole("textbox", { name: "테스트 내용·수행 가이드" })
      .fill(
        "해지 요청 전문에 사유코드 07~09 를 더했습니다. 테스트 서버에서 확인해 주세요.",
      );
    const items = page.getByTestId("item-card");
    await items
      .nth(0)
      .getByRole("textbox", { name: "요구사항" })
      .fill("해지 요청 전문 처리 확인");
    await page.getByRole("button", { name: "요구사항 더하기" }).click();
    await items
      .nth(1)
      .getByRole("textbox", { name: "요구사항" })
      .fill("해지 결과 통보 전문 확인");
    await items
      .nth(1)
      .getByRole("textbox", { name: "확인 사항" })
      .fill("결과코드 0000 을 받는지");
    await page.getByRole("checkbox", { name: "KB국민은행 대상" }).check();
    await page.getByRole("checkbox", { name: "삼성증권 대상" }).check();
    await expect(page.getByTestId("target-summary")).toHaveText("대상 2곳");
    // 맨 위 테스트데이터 사전 요청은 늘 펼쳐져 있다. 필요 없음을 체크하면 접히고, 풀면 다시 보인다.
    const dataRequest = page.getByTestId("test-data-request");
    const skip = dataRequest.getByRole("checkbox", {
      name: /사전 요청이 필요 없습니다/,
    });
    await skip.check();
    await expect(dataRequest.getByTestId("test-data-skipped")).toBeVisible();
    await skip.uncheck();
    await dataRequest
      .getByRole("textbox", { name: "요청 사항" })
      .fill("해지 예정 계좌를 포함해 테스트 계좌 10개를 만들어 주세요.");
    await dataRequest.locator('input[type="file"]').setInputFiles({
      name: "테스트데이터 양식.csv",
      mimeType: "text/csv",
      buffer: Buffer.from("계좌번호,예금주,상태\n"),
    });
    await page
      .locator('input[type="file"]')
      .nth(1)
      .setInputFiles({
        name: "테스트가이드.pdf",
        mimeType: "application/pdf",
        buffer: Buffer.from("%PDF-1.4 sample"),
      });
    await shot(page, "test-plan-editor");

    await page.getByRole("button", { name: "저장" }).click();
    await expect(page).toHaveURL(/\/account-info\/tests\/\d+$/);
    await expect(page.getByRole("heading", { name: title })).toBeVisible();
    await expect(page.getByTestId("test-plan-state")).toHaveText("진행 중");
    await expect(page.getByTestId("plan-progress")).toContainText("0%");
    const targets = page.getByTestId("target-table");
    await expect(targets).toContainText("KB국민은행");
    await expect(targets).toContainText("삼성증권");
    await expect(targets).toContainText("미생성");
    await expect(page.getByTestId("test-data-request-bar")).toContainText(
      "생성 완료 0/2",
    );
    await shot(page, "test-plan-detail");

    // 알림 설정: 만든 사람(홍성민)에 김태호을 더한다.
    await page.getByRole("link", { name: "알림 설정" }).click();
    await expect(
      page.getByRole("heading", { name: "결과 등록 알림 설정" }),
    ).toBeVisible();
    await expect(
      page.getByRole("checkbox", { name: "홍성민 알림 받기" }),
    ).toBeChecked();
    await page.getByRole("checkbox", { name: "김태호 알림 받기" }).check();
    await expect(page.getByTestId("recipient-count")).toHaveText("2명 선택");
    await shot(page, "test-plan-recipients");
    await page.getByRole("button", { name: "저장" }).click();
    await expect(page).toHaveURL(/tab=content/);
    await expect(page.getByTestId("plan-recipients")).toContainText("김태호");
    await expect(page.getByText("테스트가이드.pdf")).toBeVisible();

    // 대상 기관 담당자에게 열림 메일이 간다.
    const mails = await mailbox(request, "kim.minjun@kb.example.com");
    expect(
      mails.some((m) =>
        m.subject.includes(`${title} 기관 테스트가 열렸습니다`),
      ),
    ).toBeTruthy();
  });

  test("기관 담당자가 결과를 등록하면 수행률이 집계되고, 결제원 의견과 함께 모두 완료되면 테스트 수행 완료다", async ({
    page,
    request,
  }) => {
    const title = `E2E 해지 결과 통보 ${Date.now()}`;
    const id = await createAsHong(request, title);

    // 기관 담당자: 정상과 오류를 고른다. 오류는 특이사항(오류내용)이 있어야 저장된다.
    await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
    await page.goto(`/account-info/tests/${id}`);
    await expect(page.getByRole("heading", { name: title })).toBeVisible();
    await expect(page.getByTestId("target-table")).toHaveCount(0);
    const first = page.getByTestId("item-001");
    const second = page.getByTestId("item-002");
    await first.getByText("정상", { exact: true }).click();
    await second.getByText("오류", { exact: true }).click();
    await page.getByRole("button", { name: "결과 저장 (2)" }).click();
    await expect(
      second.getByText("오류면 특이사항에 오류내용을 써 주세요."),
    ).toBeVisible();
    await second
      .getByRole("textbox", { name: "002 특이사항" })
      .fill("결과코드 0000 대신 9999 가 내려옵니다.");
    await page.getByRole("button", { name: "결과 저장 (2)" }).click();
    await expect(page.getByTestId("target-progress")).toHaveText("50%");
    await expect(page.getByTestId("plan-progress")).toContainText("50%");
    await shot(page, "test-plan-results-organization");

    // 결제원 담당자: 알림을 받고, 001 은 완료 확인, 002 는 미니 수행률과 특이사항을 남긴다.
    await loginAs(page, "홍성민 (결제원 · 현업)로 로그인");
    const notifications = (await (
      await page.request.get("/api/v1/notifications")
    ).json()) as {
      content: { title: string }[];
    };
    expect(
      notifications.content.some((n) =>
        n.title.includes(`KB국민은행 계좌통합관리 ${title} 결과 등록`),
      ),
    ).toBeTruthy();
    await page.goto(`/account-info/tests/${id}`);
    await expect(page.getByTestId("awaiting-review")).toHaveText(
      "확인할 결과 2",
    );
    await page
      .getByTestId("item-001")
      .getByRole("button", { name: "완료 확인" })
      .click();
    await expect(
      page.getByTestId("item-001").getByTestId("item-completed"),
    ).toBeVisible();
    await page
      .getByTestId("item-002")
      .getByRole("button", { name: "002 의견" })
      .click();
    const dialog = page.getByRole("dialog");
    // 의견을 쓰는 사람은 "결제원(이름 직급)" 으로 보인다.
    await expect(dialog).toContainText("결제원(홍성민 과장)");
    await dialog.getByRole("textbox", { name: "미니 수행률" }).fill("30");
    await dialog
      .getByRole("textbox", { name: "특이사항" })
      .fill("003 전문에 대해 예외사항 발생: 결과코드 매핑 확인이 필요합니다.");
    await page.waitForTimeout(300);
    await shot(page, "test-plan-feedback");
    await dialog.getByRole("button", { name: "저장" }).click();
    await expect(dialog).toBeHidden();
    await expect(
      page.getByTestId("item-002").getByTestId("item-feedback"),
    ).toContainText("미니 수행률 30%");
    await expect(
      page.getByTestId("item-002").getByTestId("item-feedback-author"),
    ).toHaveText("결제원(홍성민 과장)");
    await expect(page.getByTestId("target-progress")).toHaveText("65%");
    await shot(page, "test-plan-results-kftc");

    // 기관 담당자가 고쳐 다시 등록하면 결제원 의견은 "다시 확인" 이 된다.
    await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
    await page.goto(`/account-info/tests/${id}`);
    await page
      .getByTestId("item-002")
      .getByText("정상", { exact: true })
      .click();
    await page
      .getByTestId("item-002")
      .getByRole("textbox", { name: "002 특이사항" })
      .fill("결과코드 매핑을 고쳐 다시 확인했습니다.");
    await page.getByRole("button", { name: "결과 저장 (1)" }).click();
    await expect(
      page.getByTestId("item-002").getByText("다시 확인", { exact: true }),
    ).toBeVisible();

    // 결제원 담당자가 다시 보고 완료 확인하면 모든 요구사항이 완료라 테스트 수행 완료다.
    await loginAs(page, "홍성민 (결제원 · 현업)로 로그인");
    await page.goto(`/account-info/tests/${id}`);
    await page
      .getByTestId("item-002")
      .getByRole("button", { name: "완료 확인" })
      .click();
    await expect(page.getByTestId("test-plan-state")).toHaveText("수행 완료");
    await expect(page.getByTestId("plan-progress")).toContainText("100%");
    await expect(
      page.getByTestId("item-002").getByTestId("item-completed"),
    ).toHaveText("테스트 수행 완료");
    await shot(page, "test-plan-completed");

    // 변경 이력: 기관별 결과 이력에 등록과 결제원 의견이 남는다.
    await page.getByRole("tab", { name: "변경 이력" }).click();
    await page.getByRole("textbox", { name: "이력 대상" }).click();
    await page.getByRole("option", { name: "KB국민은행 결과" }).click();
    await expect(
      page.getByText("KB국민은행 결제원 의견").first(),
    ).toBeVisible();
    await expect(page.getByText("KB국민은행 결과 등록").first()).toBeVisible();
  });

  test("결제원이 수행 전에 테스트데이터를 요청하면 기관이 양식을 내려받아 생성 여부와 테스트데이터를 올리고 결제원이 현황을 본다", async ({
    page,
    request,
  }) => {
    const title = `E2E 테스트데이터 ${Date.now()}`;
    const id = await createAsHong(request, title, {
      dueDate: seoulDate(0),
      instruction: "해지 예정 계좌를 포함해 테스트 계좌 10개",
    });
    const form = await request.post("/api/v1/attachments", {
      multipart: {
        ownerType: "TEST_DATA_FORM",
        ownerId: String(id),
        file: {
          name: "테스트데이터 회신 양식.csv",
          mimeType: "text/csv",
          buffer: Buffer.from("계좌번호,예금주,상태\n"),
        },
      },
      headers: await csrf(request),
    });
    expect(form.ok(), await form.text()).toBeTruthy();

    // 기관 담당자: 결제원 양식을 보고 생성 완료와 메모를 저장한 뒤 만든 파일을 올린다.
    await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
    await page.goto(`/account-info/tests/${id}`);
    const card = page.getByTestId("test-data");
    await expect(card).toContainText("테스트데이터 회신 양식.csv");
    await expect(card).toContainText("미생성");
    await expect(card.getByTestId("test-data-due")).toHaveText(seoulDate(0));
    await expect(card).toContainText(
      "해지 예정 계좌를 포함해 테스트 계좌 10개",
    );
    await card.getByText("생성 완료", { exact: true }).click();
    await card
      .getByRole("textbox", { name: "메모" })
      .fill("테스트 계좌 10개(해지 예정 2개 포함)");
    await card.getByRole("button", { name: "테스트데이터 저장" }).click();
    await expect(card.getByText("KB국민은행(김민준 대리) ·")).toBeVisible();
    await card.locator("input[type=file]").setInputFiles({
      name: "KB 테스트데이터.csv",
      mimeType: "text/csv",
      buffer: Buffer.from("계좌번호,예금주,상태\n000-00-000000,홍길동,정상\n"),
    });
    await expect(card).toContainText("KB 테스트데이터.csv");
    await shot(page, "test-data-organization");

    // 결제원 담당자: 기관별 현황에 생성 완료가 보이고, 기관 카드에서 메모와 파일을 본다. 매터모스트 알림도 받았다.
    await loginAs(page, "홍성민 (결제원 · 현업)로 로그인");
    const notifications = (await (
      await page.request.get("/api/v1/notifications")
    ).json()) as {
      content: { title: string }[];
    };
    expect(
      notifications.content.some((n) =>
        n.title.includes(`${title} 테스트데이터 생성 완료`),
      ),
    ).toBeTruthy();
    await page.goto(`/account-info/tests/${id}`);
    await expect(page.getByTestId("target-table")).toContainText("생성 완료");
    await expect(page.getByTestId("test-data-request-bar")).toContainText(
      "생성 완료 1/1",
    );
    await expect(
      page
        .getByTestId("test-data-request-bar")
        .getByRole("button", { name: /다시 요청/ }),
    ).toHaveCount(0);
    await expect(card).toContainText("테스트 계좌 10개(해지 예정 2개 포함)");
    await expect(card).toContainText("KB 테스트데이터.csv");
    await expect(
      card.getByRole("button", { name: "테스트데이터 저장" }),
    ).toHaveCount(0);
    await shot(page, "test-data-kftc");
  });

  test("결제원 의견 아래에서 결제원과 기관 담당자가 답글을 이어 달고 상대에게 알림이 간다", async ({
    page,
    request,
  }) => {
    const title = `E2E 의견 답글 ${Date.now()}`;
    const id = await createAsHong(request, title);

    await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
    await page.goto(`/account-info/tests/${id}`);
    const first = page.getByTestId("item-001");
    await first.getByText("정상", { exact: true }).click();
    await page.getByRole("button", { name: "결과 저장 (1)" }).click();
    await expect(page.getByTestId("target-progress")).toHaveText("50%");
    // 결제원 의견이 없으면 답글을 달 곳이 없다.
    await expect(first.getByRole("button", { name: "001 답글" })).toHaveCount(
      0,
    );

    // 결제원 담당자가 의견을 남기고 이어서 답글로 묻는다.
    await loginAs(page, "홍성민 (결제원 · 현업)로 로그인");
    await page.goto(`/account-info/tests/${id}`);
    await first.getByRole("button", { name: "001 의견" }).click();
    const dialog = page.getByRole("dialog");
    await dialog.getByRole("textbox", { name: "미니 수행률" }).fill("20");
    await dialog
      .getByRole("textbox", { name: "특이사항" })
      .fill(
        "007 전문의 응답이 간헐적으로 오지 않고 있습니다. 확인 부탁드립니다.",
      );
    await dialog.getByRole("button", { name: "저장" }).click();
    await expect(dialog).toBeHidden();

    // 기관 담당자가 의견 아래에 답글을 단다. 입력창에는 지금 쓸 사람이 보인다.
    await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
    await page.goto(`/account-info/tests/${id}`);
    await first.getByRole("button", { name: "001 답글" }).click();
    await expect(first.getByTestId("item-comments")).toContainText(
      "KB국민은행(김민준 대리)",
    );
    await first
      .getByRole("textbox", { name: "001 답글 입력" })
      .fill("타임아웃 설정을 30초로 늘렸습니다. 다시 확인 부탁드립니다.");
    await first.getByRole("button", { name: "답글 달기" }).click();
    await expect(first.getByTestId("item-comment")).toHaveCount(1);

    // 결제원 담당자는 알림을 받고, 답글에 다시 답글을 이어 단다.
    await loginAs(page, "홍성민 (결제원 · 현업)로 로그인");
    const notifications = (await (
      await page.request.get("/api/v1/notifications")
    ).json()) as {
      content: { title: string; body: string }[];
    };
    expect(
      notifications.content.some(
        (n) =>
          n.title.includes(`${title} 의견에 답글이 있습니다`) &&
          n.body.includes("KB국민은행(김민준 대리)"),
      ),
    ).toBeTruthy();
    await page.goto(`/account-info/tests/${id}`);
    await first.getByRole("button", { name: "001 답글" }).click();
    await first
      .getByRole("textbox", { name: "001 답글 입력" })
      .fill("확인했습니다. 응답이 모두 들어옵니다. 완료 확인하겠습니다.");
    await first.getByRole("button", { name: "답글 달기" }).click();
    const comments = first.getByTestId("item-comment");
    await expect(comments).toHaveCount(2);
    await expect(comments.nth(0)).toContainText("KB국민은행(김민준 대리)");
    await expect(comments.nth(1)).toContainText("결제원(홍성민 과장)");
    await first.getByRole("button", { name: "001 답글" }).click();
    await first
      .getByRole("textbox", { name: "001 답글 입력" })
      .fill("추가로 확인할 내용이 있으면");
    await first.scrollIntoViewIfNeeded();
    await page.waitForTimeout(300);
    await first.screenshot({ path: "screenshots/test-plan-comments.png" });
  });
});
