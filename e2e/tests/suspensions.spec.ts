import {
  type APIRequestContext,
  expect,
  type Page,
  test,
} from "@playwright/test";

import { csrf, mailbox, skipContactReconfirm } from "./support/account";

// 이 파일은 공통 세션(데모 관리자) 없이 빈 브라우저로 시작한다. 기관 담당자(김민준)와 결제원 담당자(홍성민)로 로그인한다.
// 달력이 서버와 같은 날짜를 보이도록 브라우저 시간대를 서버(Asia/Seoul)에 맞춘다.
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
  await page.goto("/login");
  await page.getByRole("button", { name: button }).click();
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
  const pad = (n: number) => String(n).padStart(2, "0");
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
  await page.getByRole("button", { name: label, exact: true }).click();
  const dropdown = page
    .getByRole("dialog")
    .filter({ has: page.getByRole("spinbutton") });
  // 달력이 다 열린 뒤에 날짜를 찾는다. 열리기 전에 보면 없는 것으로 알고 다음 달로 넘긴다.
  await expect(dropdown).toBeVisible();
  const dayButton = dropdown.getByRole("button", {
    name: `${date.day} ${date.month}월 ${date.year}`,
    exact: true,
  });
  if (!(await dayButton.isVisible())) {
    // 머리의 세 버튼은 이전 달, 달 이름, 다음 달 순서다.
    await dropdown.getByRole("button").nth(2).click();
  }
  await dayButton.click();
  await dropdown.getByRole("spinbutton").first().click();
  await page.keyboard.type(hour + minute);
  await dropdown.getByRole("button").last().click();
  await expect(dropdown).toBeHidden();
}

/** 김민준으로 KB국민은행 계좌통합관리 일시중단을 API 로 등록한다. request 의 세션은 김민준으로 바뀐다. */
async function registerAsGana(
  request: APIRequestContext,
  reason: string,
): Promise<number> {
  const login = await request.post("/api/v1/auth/login", {
    data: { email: "kim.minjun@kb.example.com", password: DEMO_PASSWORD },
    headers: await csrf(request),
  });
  expect(login.ok()).toBeTruthy();
  const works = (await (await request.get("/api/v1/target-works")).json()) as {
    id: number;
    name: string;
  }[];
  const work = works.find((w) => w.name === "계좌통합관리")!;
  const date = seoulDate(4);
  const created = await request.post("/api/v1/suspensions", {
    data: {
      targetWorkIds: [work.id],
      organizationId: null,
      startAt: iso(date, "01:00"),
      endAt: iso(date, "03:00"),
      reason,
      kftcAction: "작업 시간 동안 KB국민은행 조회 요청을 차단해 주세요.",
      contacts: [
        {
          name: "김민준",
          phone: "010-0000-2101",
          email: "kim.minjun@kb.example.com",
        },
      ],
      extraNote: null,
    },
    headers: await csrf(request),
  });
  expect(created.ok(), await created.text()).toBeTruthy();
  return ((await created.json()) as { id: number }).id;
}

test.describe("라. 일시중단", () => {
  test.beforeEach(async ({ page }) => skipContactReconfirm(page));

  test("기관 담당자가 양식으로 일시중단을 등록하면 검토 대기가 되고 결제원 담당자에게 메일이 간다", async ({
    page,
    request,
  }) => {
    await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
    await page
      .getByRole("navigation")
      .getByRole("link", { name: "일시중단", exact: true })
      .click();
    await expect(page).toHaveURL(/\/account-info\/suspensions$/);
    await expect(page.getByRole("heading", { name: "일시중단" })).toBeVisible();
    // 기관 담당자는 자기 기관 것만 보고, 기관 고르기는 없다.
    const table = page.getByTestId("suspension-table");
    await expect(table).toContainText("KB국민은행");
    await expect(table).not.toContainText("신한은행");
    await expect(page.getByRole("textbox", { name: "기관" })).toHaveCount(0);
    await shot(page, "suspension-list-organization");

    await page.getByRole("link", { name: "일시중단 등록" }).click();
    await expect(
      page.getByRole("heading", { name: "일시중단 등록" }),
    ).toBeVisible();
    await expect(page.getByRole("textbox", { name: "기관" })).toHaveValue(
      "KB국민은행",
    );
    await expect(page.getByRole("textbox", { name: "이름" })).toHaveValue(
      "김민준",
    );

    // 달력은 일요일부터 시작한다.
    await page.getByRole("button", { name: "중단 시작", exact: true }).click();
    const calendar = page
      .getByRole("dialog")
      .filter({ has: page.getByRole("spinbutton") });
    await expect(calendar).toBeVisible();
    await expect(calendar.locator("thead th").first()).toHaveText("일");
    await expect(calendar.locator("thead th").last()).toHaveText("토");
    await shot(page, "suspension-editor-calendar");
    await page.keyboard.press("Escape");
    await expect(calendar).toBeHidden();

    const reason = `E2E 계정계 패치 작업 ${Date.now()}`;
    const date = seoulDate(3);
    await pickDateTime(page, "중단 시작", date, "02", "00");
    await pickDateTime(page, "중단 종료", date, "04", "30");
    await page
      .getByRole("textbox", { name: "중단 이유(작업 내용)" })
      .fill(reason);
    await page
      .getByRole("textbox", { name: "결제원 조치사항" })
      .fill("작업 시간 동안 KB국민은행 조회 요청을 차단해 주세요.");
    await page.getByRole("textbox", { name: "연락처" }).fill("010-0000-2101");
    // 담당자는 행을 더해 여러 명을 적는다.
    await page.getByRole("button", { name: "행 추가" }).click();
    await expect(page.getByTestId("suspension-contact-row")).toHaveCount(2);
    await page.getByRole("textbox", { name: "이름" }).nth(1).fill("한예린");
    await page
      .getByRole("textbox", { name: "연락처" })
      .nth(1)
      .fill("010-0000-2102");
    await page.locator('input[type="file"]').setInputFiles({
      name: "작업공문.pdf",
      mimeType: "application/pdf",
      buffer: Buffer.from("%PDF-1.4 sample"),
    });
    await expect(page.getByRole("list", { name: "올릴 파일" })).toContainText(
      "작업공문.pdf",
    );
    // 대상 업무는 레일과 별개인 기준정보(수십 개)를 묶음별로 보이고, 찾아서 여러 개 고른다. 세부업무 칸은 없다.
    await expect(page.getByRole("textbox", { name: "세부업무" })).toHaveCount(
      0,
    );
    const businesses = page.getByTestId("suspension-businesses");
    expect(await businesses.getByRole("checkbox").count()).toBeGreaterThan(30);
    await expect(
      businesses.getByText("지급결제", { exact: true }),
    ).toBeVisible();
    await expect(page.getByTestId("suspension-businesses-count")).toHaveText(
      "고른 업무 0개",
    );
    const search = page.getByRole("textbox", { name: "대상 업무 찾기" });
    await search.fill("계좌통합");
    await expect(businesses.getByRole("checkbox")).toHaveCount(1);
    await businesses
      .getByRole("checkbox", { name: "계좌통합관리", exact: true })
      .check();
    await search.fill("CMS");
    await businesses
      .getByRole("checkbox", { name: "CMS", exact: true })
      .check();
    await shot(page, "suspension-editor-search");
    await page.getByRole("button", { name: "찾기 지우기" }).click();
    await expect(page.getByTestId("suspension-businesses-count")).toHaveText(
      "고른 업무 2개",
    );
    await shot(page, "suspension-editor");

    await page.getByRole("button", { name: "저장" }).click();
    await expect(page).toHaveURL(/\/account-info\/suspensions\/\d+$/);
    await expect(
      page.getByRole("heading", { name: "KB국민은행 일시중단" }),
    ).toBeVisible();
    await expect(page.getByTestId("suspension-status")).toHaveText("검토 대기");
    await expect(page.getByTestId("suspension-period")).toContainText(
      "02:00 ~ 04:30",
    );
    await expect(page.getByText("작업공문.pdf")).toBeVisible();
    await expect(page.getByTestId("suspension-target-works")).toHaveText(
      "계좌통합관리, CMS",
    );
    await expect(page.getByTestId("suspension-businesses")).toHaveText(
      "계좌통합관리, CMS",
    );
    const contacts = page.getByTestId("suspension-contacts");
    await expect(contacts.getByText(/김민준/)).toBeVisible();
    await expect(contacts.getByText(/한예린/)).toBeVisible();
    // 기관 담당자는 확인·확정은 못 하고, 자기가 올린 건은 고치고 지울 수 있다.
    await expect(page.getByRole("button", { name: "확인 시작" })).toHaveCount(
      0,
    );
    await expect(page.getByRole("link", { name: "고치기" })).toBeVisible();
    await expect(page.getByRole("button", { name: "삭제" })).toBeVisible();
    await shot(page, "suspension-detail-organization");

    // 고른 대상 업무와 관련된 업무의 결제원 담당자에게 등록 메일이 간다(매터모스트는 발송함에 함께 남는다).
    for (const email of [
      "hong.sungmin@kftc.example.com",
      "park.cms@kftc.example.com",
    ]) {
      const mails = await mailbox(request, email);
      expect(
        mails.some((m) =>
          m.subject.includes(
            "KB국민은행 계좌통합관리, CMS 일시중단이 등록되었습니다",
          ),
        ),
      ).toBeTruthy();
    }
  });

  test("기관 담당자는 맡지 않은 업무, 관련 업무가 없는 업무도 고르고, 관련 업무가 없으면 결제원 운영·상황실이 알림을 받는다", async ({
    page,
    request,
  }) => {
    await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
    await page.goto("/account-info/suspensions/new");
    await expect(
      page.getByRole("heading", { name: "일시중단 등록" }),
    ).toBeVisible();
    const businesses = page.getByTestId("suspension-businesses");
    await page.getByRole("textbox", { name: "대상 업무 찾기" }).fill("공동망");
    await businesses
      .getByRole("checkbox", { name: "타행환공동망", exact: true })
      .check();

    const reason = `E2E 대외계 회선 작업 ${Date.now()}`;
    const date = seoulDate(5);
    await pickDateTime(page, "중단 시작", date, "01", "00");
    await pickDateTime(page, "중단 종료", date, "02", "00");
    await page
      .getByRole("textbox", { name: "중단 이유(작업 내용)" })
      .fill(reason);
    await page.getByRole("textbox", { name: "연락처" }).fill("010-0000-2101");
    await page.getByRole("button", { name: "저장" }).click();

    // 관련 업무가 없으니 지금 업무 영역에서 상세를 연다. 자기 기관 건이라 보이고, 자기가 올린 건이라 고칠 수 있다.
    await expect(page).toHaveURL(/\/account-info\/suspensions\/\d+$/);
    await expect(
      page.getByRole("heading", { name: "KB국민은행 일시중단" }),
    ).toBeVisible();
    await expect(page.getByTestId("suspension-target-works")).toHaveText(
      "타행환공동망",
    );
    await expect(page.getByTestId("suspension-businesses")).toContainText(
      "없음",
    );
    await expect(page.getByRole("link", { name: "고치기" })).toBeVisible();
    await shot(page, "suspension-detail-unlinked");

    // 업무 담당(홍성민)이 아니라 전 업무를 보는 결제원 운영·상황실이 받는다.
    for (const email of [
      "kim.ops@kftc.example.com",
      "lee.control@kftc.example.com",
    ]) {
      const mails = await mailbox(request, email);
      expect(
        mails.some((m) =>
          m.subject.includes(
            "KB국민은행 타행환공동망 일시중단이 등록되었습니다",
          ),
        ),
      ).toBeTruthy();
    }
    const hong = await mailbox(request, "hong.sungmin@kftc.example.com");
    expect(
      hong.some((m) =>
        m.subject.includes("KB국민은행 타행환공동망 일시중단이 등록되었습니다"),
      ),
    ).toBeFalsy();
  });

  test("결제원 담당자가 확인을 시작하고 조치 및 인수인계 사항을 써서 확정하고, 기관별 이력과 취합을 본다", async ({
    page,
    request,
  }) => {
    const reason = `E2E 인터넷뱅킹 점검 ${Date.now()}`;
    const id = await registerAsGana(request, reason);

    await loginAs(page, "홍성민 (결제원 · 현업)로 로그인");
    await page
      .getByRole("navigation")
      .getByRole("link", { name: "일시중단", exact: true })
      .click();
    await expect(page.getByTestId("pending-review-count")).toContainText(
      "확정 전",
    );
    // 진행 중에는 전 기관이 보인다.
    await expect(page.getByTestId("suspension-table")).toContainText(
      "KB국민은행",
    );
    await shot(page, "suspension-list");

    await page
      .getByRole("group", { name: "시기" })
      .getByText(/예정 \d/)
      .click();
    await expect(page).toHaveURL(/period=UPCOMING/);
    const table = page.getByTestId("suspension-table");
    await expect(table).toContainText(reason);
    await table.getByRole("button").filter({ hasText: reason }).click();
    await expect(page).toHaveURL(
      new RegExp(`/account-info/suspensions/${id}$`),
    );
    // 주소가 바뀐 직후에는 목록이 아직 남아 있을 수 있어, 상세 머리가 뜬 뒤에 상태를 본다.
    await expect(
      page.getByRole("heading", { name: "KB국민은행 일시중단" }),
    ).toBeVisible();
    await expect(page.getByTestId("suspension-status")).toHaveText("검토 대기");

    // 확인 시작: 검토 중이 되고 확인자로 이름이 남는다.
    await page.getByRole("button", { name: "확인 시작" }).click();
    await page
      .getByRole("dialog", { name: "확인 시작" })
      .getByRole("button", { name: "확인 시작" })
      .click();
    await expect(page.getByTestId("suspension-status")).toHaveText("검토 중");
    await expect(page.getByTestId("suspension-reviewers")).toContainText(
      "홍성민",
    );

    // 조치 및 인수인계 사항을 쓰면 확정된다.
    await page.getByRole("button", { name: "조치 쓰고 확정" }).click();
    const handover = page.getByRole("dialog", { name: "조치 쓰고 확정" });
    await handover
      .getByRole("textbox", { name: "조치 및 인수인계 사항" })
      .fill(
        "운영팀이 01:00 에 KB국민은행 조회를 차단하고, 재개 연락을 받으면 풀고 상황실에 알립니다.",
      );
    await page.waitForTimeout(300);
    await shot(page, "suspension-handover");
    await handover.getByRole("button", { name: "확정하기" }).click();
    await expect(page.getByTestId("suspension-status")).toHaveText("확정");
    await expect(page.getByTestId("suspension-handover")).toContainText(
      "운영팀이 01:00 에",
    );
    await expect(
      page.getByText(/결제원\(홍성민 과장\) · .* 확정/),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "조치 사항 고치기" }),
    ).toBeVisible();
    await shot(page, "suspension-detail");

    // 변경 이력
    await page.getByRole("tab", { name: "변경 이력" }).click();
    await expect(
      page.getByText("조치 및 인수인계", { exact: false }).first(),
    ).toBeVisible();

    // 기관별 과거 이력: KB국민은행을 골라 지난 일시중단을 본다.
    await page
      .getByRole("navigation")
      .getByRole("link", { name: "일시중단", exact: true })
      .click();
    await page.getByRole("textbox", { name: "기관" }).click();
    await page.getByRole("option", { name: "KB국민은행" }).click();
    await expect(page).toHaveURL(/org=\d+/);
    await page
      .getByRole("group", { name: "시기" })
      .getByText(/지난 \d/)
      .click();
    const history = page.getByTestId("suspension-table");
    await expect(history).toContainText("재해복구 전환 훈련");
    await expect(history).not.toContainText("신협");
    await shot(page, "suspension-organization-history");

    // 전 영업일 취합을 확인 뒤 운영·관제 채널로 수동으로 보낸다.
    await page.getByRole("button", { name: "수동으로 보내기" }).click();
    const confirmSend = page.getByRole("dialog", {
      name: "전 영업일 취합 수동으로 보내기",
    });
    await expect(confirmSend).toContainText("오후 4시 자동 발송");
    await confirmSend.getByRole("button", { name: "보내기" }).click();
    const digest = page.getByRole("dialog", {
      name: "전 영업일 취합을 보냈습니다",
    });
    await expect(digest.getByTestId("digest-body")).toContainText("일시중단");
    await expect(digest).toContainText("~kftc-ops");
    await page.waitForTimeout(300);
    await shot(page, "suspension-digest");
  });
});

test("기관 담당자는 같은 업무 다른 기관의 확정 일정만 기관·일시·대상 업무로 본다", async ({
  page,
}) => {
  await skipContactReconfirm(page);
  await loginAs(page, "김민준 (참가기관 · 현업)로 로그인");
  await page.goto("/account-info/suspensions");
  const card = page.getByTestId("other-schedules");
  await expect(card.getByText("저축은행중앙회")).toBeVisible();
  // 자기 기관 건과 확정 전 건(신한은행·삼성증권 검토 대기·검토 중)은 빠지고, 중단 이유는 보이지 않는다.
  await expect(card.getByText("KB국민은행")).toHaveCount(0);
  await expect(card.getByText("신한은행")).toHaveCount(0);
  await expect(card.getByText(/리허설/)).toHaveCount(0);
  await shot(page, "suspension-other-schedules");
});
