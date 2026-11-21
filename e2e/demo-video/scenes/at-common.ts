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
export const AT_MEETING_NOTICE = '자동이체 해지 결과 배치파일 개선 실무 회의 개최 안내';
export const AT_MEETING_REQUEST = '자동이체 해지 결과 배치파일 개선 실무 회의 참석 회신';
export const AT_SURVEY = '자동이체 해지 결과 배치파일 개선 의견조사';
export const AT_TEST = '자동이체 해지 결과 배치파일 개선';
/** 운영 장면에서 김태호가 보내는 확인 요청(@신한은행 뒤에 붙는다). */
export const AT_OPS_MESSAGE = '13시 해지 결과 파일이 아직 들어오지 않았습니다. 확인 부탁드립니다.';
export const AT_OPS_REPLY = '13시 20분에 재송신했습니다.';

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

/** 결제원 업무 담당자(문서준 과장) 시점. */
export async function asMoon(d: Director) {
  await d.switchSession(MOON);
  await d.who('kftc', '결제원', '문서준 과장 · 자동이체 업무 담당');
}

/** 결제원 운영 담당자(김태호 대리) 시점. */
export async function asOps(d: Director) {
  await d.switchSession(OPS);
  await d.who('kftc', '결제원', '김태호 대리 · 운영 담당');
}

/** 참가기관 담당자(신한은행 이준호 과장) 시점. 6개월 확인 창은 띄우지 않는다. */
export async function asLee(d: Director) {
  await d.skipReconfirm();
  await d.switchSession(LEE);
  await d.who('org', '신한은행', '이준호 과장 · 참가기관 담당자');
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
