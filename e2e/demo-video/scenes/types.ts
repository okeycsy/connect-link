import type { Page } from '@playwright/test';

import type { CardSpec } from '../cards.ts';
import type { Director } from '../helpers.ts';

/**
 * 시연영상 장면. 장(chapter)마다 파일 하나(01-intro.ts …)에 모아 두고 index.ts 가 순서대로 잇는다.
 *
 * - card: 큰 글자 화면. duration(ms) 동안 보인다. narration 은 카드가 뜰 때 읽는 한 줄.
 * - app: 실제 화면을 조작하며 녹화한다. 강조는 d.focus(), 내레이션은 읽기 시작할 동작 바로 앞에 d.say().
 * - summary: 시나리오 문서의 "화면·동선" 칸. 녹화 뒤 docs.ts 가 장면표를 만들 때 쓴다.
 */
export type Scene =
  | { id: string; kind: 'card'; duration: number; card: CardSpec; narration?: string }
  | { id: string; kind: 'app'; narration?: string; summary: string; run: (d: Director, page: Page) => Promise<void> };

/** 장: 다시 찍고 다시 붙이는 단위. `record.ts --only=<장 id>` 로 그 장만 다시 녹화한다. */
export type Chapter = { id: string; title: string; scenes: Scene[] };

export const HONG = 'hong.sungmin@kftc.example.com';
export const KIM = 'kim.minjun@kb.example.com';
/** 섹션 이동(구글폼식 분기)이 있는 샘플 의견 요청. */
export const BRANCHED_OPINION = '내 계좌 한눈에 응답 전문 항목 추가 검토';
export const BRANCHED_OPINION_ID = 3;
export const SEEDED_TEST = '본인계좌 일괄지급정지 전문 개선 2차';
/** 소통 채널 장면에서 결제원이 보내는 긴급 확인 요청. */
export const URGENT_MESSAGE = '내 계좌 한눈에 조회 전문 응답 지연 장애가 발생했습니다. 귀 기관 영향 여부를 긴급히 확인해 주세요.';
/** 참석 회신 의견 요청을 이어 둔 회의 개최 안내 공지. */
export const MEETING_NOTICE = '계좌통합관리업무 전문 변경 회의 개최 안내';

/** 결제원 담당자(홍성민 과장) 시점으로 바꾼다. 머리 막대에 갈색 알약이 뜬다. */
export async function asKftc(d: Director) {
  await d.switchSession(HONG);
  await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
}

/** 참가기관 담당자(KB국민은행 김민준 대리) 시점으로 바꾼다. 머리 막대에 파란 알약이 뜬다. */
export async function asOrg(d: Director, opts: { reconfirm?: boolean } = {}) {
  if (!opts.reconfirm) await d.skipReconfirm();
  await d.switchSession(KIM);
  await d.who('org', 'KB국민은행', '김민준 대리 · 참가기관 담당자');
}
