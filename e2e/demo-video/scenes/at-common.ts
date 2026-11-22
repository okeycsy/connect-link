import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import type { Locator } from '@playwright/test';

import type { Director } from '../helpers.ts';

/**
 * 2차 심사 발표 영상(자동이체 해지 결과 배치파일 개선 과제, scenario-autotransfer.md) 장면들이 함께 쓰는 것.
 * 과제 데이터는 녹화 전에 setup-autotransfer.ts 가 만들고 out/autotransfer.json 에 id 를 남긴다.
 */
export const AT_SLUG = 'auto-transfer';
export const MOON = 'moon.seojun@kftc.example.com';
export const OPS = 'kim.ops@kftc.example.com';
export const LEE = 'lee.junho@shinhan.example.com';
export const LIM = 'lim.chaewon@samsung.example.com';
export const AT_MEETING_NOTICE = '자동이체 해지 결과 배치파일 개선 실무 회의 개최 안내';
export const AT_MEETING_REQUEST = '자동이체 해지 결과 배치파일 개선 실무 회의 참석 회신';
export const AT_SURVEY = '자동이체 해지 결과 배치파일 개선 의견조사';
export const AT_TEST = '자동이체 해지 결과 배치파일 개선';
/** 운영 장면에서 김태호가 단체방에 올리는 지연 안내(발표자료 6장, SHY 2026-11-22). */
export const AT_OPS_MESSAGE =
  '[지연 안내] 타 기관 수신 지연으로 오늘 해지 요청 파일이 30분가량 늦게 나갑니다. 13시 처리결과 회신은 13시 30분 이후에 부탁드립니다.';

type Ids = { meetingRequestId: number; noticeId: number; surveyId: number; testPlanId: number; orgIds: Record<string, number> };

let cached: Ids | null = null;
/** setup-autotransfer.ts 가 만든 id. 장면을 실행할 때 읽는다(장 목록만 불러올 때는 파일이 없어도 된다). */
export function atIds(): Ids {
  if (!cached) {
    const here = path.dirname(fileURLToPath(import.meta.url));
    const file = path.join(here, '..', 'out', 'autotransfer.json');
    try {
      cached = JSON.parse(readFileSync(file, 'utf8')) as Ids;
    } catch {
      throw new Error(`${file} 이 없습니다. DEMO_SCENARIO=autotransfer 로 run.sh 를 돌리거나 setup-autotransfer.ts 를 먼저 실행하세요.`);
    }
  }
  return cached;
}

/** 로그인 주체 알약을 왼쪽 아래(사이드바 빈 곳)로 옮긴다. 위쪽에 두면 상세 패널 제목과 알림 토스트를 가린다. */
const WHO_BOTTOM_LEFT =
  '#__demo-who{top:auto!important;right:auto!important;left:16px!important;bottom:16px!important}';

async function who(d: Director, kind: 'kftc' | 'org', tag: string, text: string) {
  await d.page.addInitScript((css) => {
    document.addEventListener('DOMContentLoaded', () => {
      const el = document.createElement('style');
      el.textContent = css;
      document.head.appendChild(el);
    });
  }, WHO_BOTTOM_LEFT);
  await d.who(kind, tag, text);
}

/** 결제원 업무 담당자(문서준 과장) 시점. */
export async function asMoon(d: Director) {
  await d.switchSession(MOON);
  await who(d, 'kftc', '결제원', '문서준 과장 · 자동이체 업무 담당');
}

/** 결제원 운영 담당자(김태호 대리) 시점. */
export async function asOps(d: Director) {
  await d.switchSession(OPS);
  await who(d, 'kftc', '결제원', '김태호 대리 · 운영 담당');
}

/** 참가기관 담당자(신한은행 이준호 과장) 시점. 6개월 확인 창은 띄우지 않는다. */
export async function asLee(d: Director) {
  await d.skipReconfirm();
  await d.switchSession(LEE);
  await who(d, 'org', '신한은행', '이준호 과장 · 참가기관 담당자');
}

/** 참가기관 담당자(삼성증권 임채원 과장) 시점. 최근 확인일이 6개월 지나 로그인하면 확인 창이 뜬다(prep 장면). */
export async function asLim(d: Director) {
  await d.switchSession(LIM);
  await who(d, 'org', '삼성증권', '임채원 과장 · 참가기관 담당자');
}

/**
 * 실제 시연처럼 보이게: 확대·말풍선 없이 그 요소가 화면에 들어오도록 스크롤하고 마우스를 가져가 잠깐 둔다.
 * 2차 심사 영상은 발표자가 직접 설명하므로(SHY 요청 "광고 느낌 말고 말 그대로 시연영상") 연출은 커서 움직임까지만 한다.
 */
export async function look(d: Director, targets: Locator | Locator[], hold = 700) {
  const list = Array.isArray(targets) ? targets : [targets];
  for (const t of list) await t.first().waitFor({ state: 'visible' });
  const first = list[0].first();
  const box = await first.boundingBox();
  const viewport = d.page.viewportSize();
  if (box && viewport && (box.y < 60 || box.y + Math.min(box.height, viewport.height * 0.6) > viewport.height - 20)) {
    await d.centerOn(first, 0.45);
  }
  await d.moveTo(first, { steps: 18 });
  await d.pause(hold);
}

/** 대본 v1.8(발표 5분)에 맞춘 짧은 영상용: 커서를 빨리 옮기고 덜 기다린다. */
export const FAST = { settle: 60, steps: 6 } as const;

/**
 * 처음 보는 심사위원도 따라오도록 조금 천천히(SHY 2026-11-22 "너무 빨라 정신없다"). 영상 위 대본이 영상보다 긴 장(biz·susp·ops)은
 * 대본 길이까지 영상을 늘려도 발표 시간이 늘지 않는다. dev 는 대본과 영상 길이가 비슷해 MEDIUM 만 쓴다.
 */
export const CALM = { settle: 380, steps: 14 } as const;
export const MEDIUM = { settle: 140, steps: 8 } as const;
