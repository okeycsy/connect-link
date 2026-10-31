import type { Locator, Page } from '@playwright/test';

/**
 * 장면 스크립트가 쓰는 연출 도우미. 실제 마우스를 천천히 움직이고(커서 오버레이가 따라온다),
 * 사람이 치듯 글자를 넣고, 자막을 띄우고, 화면을 확대한다.
 */
export class Director {
  readonly page: Page;
  /** 지금 강조(스포트라이트·확대) 중인지. 강조 중에 다른 곳을 강조하면 먼저 되돌린다. */
  private focused = false;
  /** 내레이션 큐: 장면 시작 뒤 몇 초에 어떤 문구를 읽기 시작하는지. record.ts 가 timeline.json 에 남기고 docs.ts 가 대본·SRT 를 만든다. */
  readonly cues: { at: number; text: string }[] = [];
  private readonly startedAt = Date.now();

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * 내레이션 큐를 남긴다. 화면은 멈추지 않는다. 이 시점에 읽기 시작할 문구를 그 동작 바로 앞에 둔다.
   * 다음 큐가 오기 전에 다 읽히도록 짧게 쓴다(예상 읽기 시간은 docs.ts 가 narration.md 에 적는다).
   */
  say(text: string) {
    this.cues.push({ at: Math.round((Date.now() - this.startedAt) / 100) / 10, text });
  }

  /** 밀리초만큼 기다린다. 장면의 호흡(숨 고르기)에 쓴다. */
  async pause(ms: number) {
    await this.page.waitForTimeout(ms);
  }

  /** 자막을 띄운다. `<b>` 로 강조할 수 있다. */
  async caption(html: string | null) {
    await this.page.evaluate((h) => window.__demo?.caption(h), html);
  }

  /** 요소 가운데(또는 치우친 지점)로 마우스를 부드럽게 옮긴다. */
  async moveTo(target: Locator, opts: { dx?: number; dy?: number; steps?: number } = {}) {
    await target.first().waitFor({ state: 'visible' });
    await target.first().scrollIntoViewIfNeeded();
    const box = await this.stableBox(target);
    const x = box.x + box.width / 2 + (opts.dx ?? 0);
    const y = box.y + box.height / 2 + (opts.dy ?? 0);
    await this.page.mouse.move(x, y, { steps: opts.steps ?? 12 });
    return { x, y };
  }

  /** 요소가 움직임(모달 열림, 스크롤 애니메이션)을 멈출 때까지 기다린 뒤 위치를 돌려준다. */
  private async stableBox(target: Locator) {
    let last = await target.first().boundingBox();
    for (let i = 0; i < 12; i++) {
      await this.pause(90);
      const next = await target.first().boundingBox();
      if (last && next && Math.abs(last.x - next.x) < 0.5 && Math.abs(last.y - next.y) < 0.5 && Math.abs(last.width - next.width) < 0.5) {
        return next;
      }
      last = next;
    }
    if (!last) throw new Error('요소 위치를 알 수 없습니다: ' + target);
    return last;
  }

  /** 마우스를 옮긴 뒤 잠깐 멈추고 누른다. 사람이 누르는 리듬이다. */
  async click(target: Locator, opts: { dx?: number; dy?: number; settle?: number; steps?: number } = {}) {
    const { x, y } = await this.moveTo(target, opts);
    await this.pause(opts.settle ?? 170);
    await this.page.mouse.down();
    await this.pause(60);
    await this.page.mouse.up();
    return { x, y };
  }

  /**
   * 누른 뒤 기대한 결과(expected 가 보임)가 나올 때까지 기다린다. 애니메이션 중에 눌려 빠진 클릭은 한 번 더 누른다.
   * 상태가 바뀌어야 하는 버튼(확정, 제출, 보내기)에 쓴다.
   */
  async clickUntil(target: Locator, expected: Locator, opts: { dx?: number; dy?: number; settle?: number; steps?: number } = {}) {
    await this.click(target, opts);
    try {
      await expected.first().waitFor({ state: 'visible', timeout: 4000 });
    } catch {
      if (await target.first().isVisible()) {
        await this.click(target, opts);
      }
      await expected.first().waitFor({ state: 'visible' });
    }
  }

  /** 입력 칸을 누르고 한 글자씩 친다. */
  async type(target: Locator, text: string, opts: { delay?: number } = {}) {
    await this.click(target);
    await this.pause(150);
    await this.page.keyboard.type(text, { delay: opts.delay ?? 55 });
  }

  /**
   * 강조: 자막이 가리키는 요소만 스포트라이트로 비추고, 잘리지 않는 배율까지 확대하고, 설명(note)을 그 옆에 붙인다.
   * 요소 여러 개를 주면 그 묶음을 한 번에 비춘다. 요소가 자라거나 움직여도 따라간다(섹션이 나타나는 회신 화면 등).
   * - scale: 확대 상한(기본 1.3). 요소가 화면에 다 들어오도록 줄어든다. 1 이면 확대 없이 비추기만 한다.
   * - hold: 주면 그만큼 보여 준 뒤 스스로 되돌린다(unfocus). 안 주면 강조한 채로 다음 조작(클릭·타자)을 이어 간다.
   * - center: 강조 전에 요소가 화면 높이의 어느 지점에 오게 스크롤할지(기본 0.5).
   */
  async focus(
    targets: Locator | Locator[],
    opts: { note?: string; scale?: number; hold?: number; pad?: number; center?: number } = {},
  ) {
    const list = Array.isArray(targets) ? targets : [targets];
    if (this.focused) await this.unfocus();
    for (const t of list) await t.first().waitFor({ state: 'visible' });
    await this.page.evaluate(() => document.querySelectorAll('[data-demo-focus]').forEach((el) => el.removeAttribute('data-demo-focus')));
    for (const t of list) await t.first().evaluate((el) => el.setAttribute('data-demo-focus', ''));
    await this.centerOnFocused(opts.center ?? 0.5);
    for (const t of list) await this.stableBox(t);
    await this.page.evaluate(
      ({ note, scale, pad }) => window.__demo?.focus({ note, scale, pad }),
      { note: opts.note ?? '', scale: opts.scale ?? 1.3, pad: opts.pad ?? 12 },
    );
    this.focused = true;
    await this.pause(380);
    if (opts.hold != null) {
      await this.pause(opts.hold);
      await this.unfocus();
    }
  }

  /** 강조 중에 설명 문구만 바꾼다. */
  async note(html: string | null) {
    await this.page.evaluate((h) => window.__demo?.note(h), html);
    await this.pause(350);
  }

  /** 강조를 끝낸다(스포트라이트·설명을 지우고 확대를 되돌린다). */
  async unfocus() {
    await this.page.evaluate(() => window.__demo?.unfocus());
    this.focused = false;
    await this.pause(340);
  }

  /** 강조 표시된 요소 묶음의 가운데가 화면 높이의 ratio 지점에 오도록 스크롤한다. */
  private async centerOnFocused(ratio: number) {
    const moved = await this.page.evaluate((r) => {
      const els = Array.from(document.querySelectorAll('[data-demo-focus]'));
      if (!els.length) return false;
      let top = Infinity;
      let bottom = -Infinity;
      for (const el of els) {
        const q = el.getBoundingClientRect();
        top = Math.min(top, q.top);
        bottom = Math.max(bottom, q.bottom);
      }
      const want = window.scrollY + (top + bottom) / 2 - window.innerHeight * r;
      const max = Math.max(0, document.documentElement.scrollHeight - window.innerHeight);
      const target = Math.min(Math.max(0, want), max);
      if (Math.abs(target - window.scrollY) < 4) return false;
      window.scrollTo({ top: target, behavior: 'smooth' });
      return true;
    }, ratio);
    if (moved) await this.pause(450);
  }

  /** 요소가 화면 높이의 ratio 지점에 오도록 부드럽게 스크롤한다(확대 전에 자리를 잡을 때). */
  async centerOn(target: Locator, ratio = 0.5) {
    await target.first().waitFor({ state: 'visible' });
    await target.first().evaluate((el, r) => {
      const rect = el.getBoundingClientRect();
      const top = window.scrollY + rect.top - (window.innerHeight * r - rect.height / 2);
      window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
    }, ratio);
    await this.pause(650);
  }

  /** 두 요소를 함께 감싸는 영역의 가운데를 중심으로 확대한다(둘 다 한 화면에 보여야 한다). */
  async zoomBetween(a: Locator, b: Locator, scale = 1.3) {
    const [ba, bb] = await Promise.all([a.first().boundingBox(), b.first().boundingBox()]);
    if (!ba || !bb) throw new Error('요소 위치를 알 수 없습니다');
    const left = Math.min(ba.x, bb.x);
    const right = Math.max(ba.x + ba.width, bb.x + bb.width);
    const top = Math.min(ba.y, bb.y);
    const bottom = Math.max(ba.y + ba.height, bb.y + bb.height);
    await this.page.evaluate(({ x, y, s }) => window.__demo?.zoom(x, y, s), {
      x: (left + right) / 2,
      y: (top + bottom) / 2,
      s: scale,
    });
  }

  /** 요소를 중심으로 확대한다. */
  async zoomTo(target: Locator, scale = 1.5) {
    const box = await target.first().boundingBox();
    if (!box) throw new Error('요소 위치를 알 수 없습니다: ' + target);
    await this.page.evaluate(
      ({ x, y, s }) => window.__demo?.zoom(x, y, s),
      { x: box.x + box.width / 2, y: box.y + box.height / 2, s: scale },
    );
  }

  async zoomAt(x: number, y: number, scale = 1.5) {
    await this.page.evaluate(({ x, y, s }) => window.__demo?.zoom(x, y, s), { x, y, s: scale });
  }

  async unzoom() {
    await this.page.evaluate(() => window.__demo?.unzoom());
  }

  async hideCursor() {
    await this.page.evaluate(() => window.__demo?.hideCursor());
  }

  /**
   * 지금 누구로 로그인했는지 오른쪽 위 알약으로 보인다(결제원 담당자인지 기관 담당자인지 한눈에).
   * 이 장면의 다음 화면 이동에도 이어진다(sessionStorage).
   */
  async who(kind: 'kftc' | 'org', tag: string, text: string) {
    const payload = { kind, tag, text };
    await this.page.addInitScript((p) => {
      try {
        window.sessionStorage.setItem('__demo.who', JSON.stringify(p));
      } catch {
        // 저장소를 못 쓰면 다음 화면에서는 안 보인다.
      }
    }, payload);
    if (!/^about:blank/.test(this.page.url())) {
      await this.page.evaluate((p) => {
        try {
          window.sessionStorage.setItem('__demo.who', JSON.stringify(p));
        } catch {
          // 위와 같다.
        }
        window.__demo?.who(p.kind, p.tag, p.text);
      }, payload);
    }
  }

  /** 커서를 화면 밖 느낌이 나는 한쪽으로 치운다. */
  async parkCursor() {
    await this.page.mouse.move(1880, 1040, { steps: 20 });
    await this.hideCursor();
  }

  /** 데모 계정 버튼으로 로그인한다. 로그인 화면이 아니면 먼저 간다. */
  async loginAs(name: string, opts: { showLoginScreen?: number } = {}) {
    const page = this.page;
    if (!/\/login/.test(page.url())) {
      await page.goto('/login');
    }
    await page.getByTestId('demo-accounts').waitFor();
    if (opts.showLoginScreen) await this.pause(opts.showLoginScreen);
    const button = page.getByRole('button', { name: new RegExp(`^${name} .*로 로그인`) });
    await button.scrollIntoViewIfNeeded();
    await this.click(button);
    await page.waitForURL(/\/home$/);
  }

  /** 김민준의 6개월 경과 확인 창을 이미 본 것으로 둔다(확인 창을 다루지 않는 장면용). */
  async skipReconfirm() {
    await this.page.addInitScript(() => {
      try {
        window.sessionStorage.setItem('contact.reconfirmShown', 'kim.minjun@kb.example.com');
      } catch {
        // 저장소를 못 쓰면 창이 뜬다.
      }
    });
  }

  /** 로그인 API 로 조용히 세션을 바꾼다(화면 전환 없이 다른 사람 시점으로 넘어갈 때). */
  async switchSession(email: string) {
    const ctx = this.page.context();
    const cookies = await ctx.cookies();
    let token = cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
    if (!token) {
      await ctx.request.get('/api/v1/health');
      token = (await ctx.cookies()).find((c) => c.name === 'XSRF-TOKEN')?.value;
    }
    const login = await ctx.request.post('/api/v1/auth/login', {
      data: { email, password: 'demo1234!' },
      headers: { 'X-XSRF-TOKEN': token! },
    });
    if (!login.ok()) throw new Error('로그인 실패: ' + email + ' ' + (await login.text()));
  }
}

declare global {
  interface Window {
    __demo?: {
      caption(html: string | null): void;
      focus(opts: { note?: string; scale?: number; pad?: number }): boolean;
      note(html: string | null): void;
      unfocus(): void;
      zoom(x: number, y: number, scale: number): void;
      unzoom(): void;
      hideCursor(): void;
      who(kind: 'kftc' | 'org', tag: string, text: string): void;
    };
  }
}
