import type { Page } from '@playwright/test';

import type { CardSpec } from './cards.ts';
import type { Director } from './helpers.ts';

/**
 * 시연영상 장면. 순서대로 녹화해 이어 붙인다(record.ts → compose.ts).
 * 문구·순서는 시나리오 문서(프로젝트 파일 demo-video/scenario-v2.md)의 장면표와 같다. 바꾸면 둘 다 고친다.
 *
 * - card: 큰 글자 화면. duration(ms) 동안 보인다.
 * - app: 실제 화면을 조작하며 녹화한다. 자막은 d.caption().
 */
export type Scene =
  | { id: string; kind: 'card'; duration: number; card: CardSpec; narration?: string }
  | { id: string; kind: 'app'; narration?: string; run: (d: Director, page: Page) => Promise<void> };

const HONG = 'hong.sungmin@kftc.example.com';
const KIM = 'kim.minjun@kb.example.com';
/** 섹션 이동(구글폼식 분기)이 있는 샘플 의견 요청. */
const BRANCHED_OPINION = '내 계좌 한눈에 응답 전문 항목 추가 검토';
const BRANCHED_OPINION_ID = 3;
const SEEDED_TEST = '본인계좌 일괄지급정지 전문 개선 2차';

export const SCENES: Scene[] = [
  // ───────────── 1. 문제 (업무 관련성)
  {
    id: 'p1',
    kind: 'card',
    duration: 3200,
    card: { tone: 'dark', title: '담당자가 바뀌었는데,\n아무도 몰랐다.' },
    narration: '참가기관 담당자가 바뀌어도, 우리는 연락하는 순간에야 압니다.',
  },
  {
    id: 'p2',
    kind: 'card',
    duration: 3500,
    card: { tone: 'dark', title: '메일로 묻고, 엑셀에 옮기고,\n전화로 다시 확인합니다.' },
    narration: '연락처도, 의견도, 테스트 결과도, 일시중단 일정도. 메일과 엑셀, 전화로 흩어져 있습니다.',
  },
  {
    id: 'p3',
    kind: 'card',
    duration: 4300,
    card: { tone: 'dark', stat: { value: 305, unit: '시간', label: '업무 하나에서, 해마다 이 반복 수작업에 쓰는 시간' } },
    narration: '업무 하나에서만, 해마다 305시간이 이런 반복 수작업에 들어갑니다.',
  },

  // ───────────── 2. 소개 (확산 가능성)
  {
    id: 'intro',
    kind: 'card',
    duration: 4000,
    card: {
      tone: 'light',
      image: { src: '/brand/logo-mascot.png', width: 430 },
      title: '결제원과 참가기관을 잇는\n하나의 포털',
    },
    narration: '그래서 하나의 포털을 만들었습니다. 연결이입니다.',
  },
  {
    id: 'login-home',
    kind: 'app',
    narration:
      '결제원 담당자와 기관 담당자가 같은 입구로 들어옵니다. 로그인하면 지금 할 일만 보입니다. 계좌통합관리, 자동이체, 대출이동, CMS. 어느 업무든 같은 메뉴, 같은 구조입니다.',
    run: async (d, page) => {
      await page.goto('/login');
      await page.getByTestId('demo-accounts').waitFor();
      await d.pause(1000);
      await d.caption('결제원도, 참가기관도 같은 입구. <b>회사 이메일 하나</b>로.');
      await d.pause(1100);
      await d.loginAs('홍성민');
      await page.getByRole('heading', { name: /안녕하세요/ }).waitFor();
      await d.pause(300);
      await d.caption('로그인하면 <b>지금 할 일</b>만 보입니다.');
      await d.zoomTo(page.getByText('다가오는 일시중단', { exact: true }), 1.3);
      await d.pause(2000);
      await d.unzoom();
      await d.pause(500);
      await d.caption('업무 4개 × 기능 6개, <b>같은 구조</b>.');
      const nav = page.getByRole('navigation');
      for (const name of ['연락처', '의견 요청', '기관 테스트', '일시중단', '소통 채널']) {
        await d.moveTo(nav.getByRole('link', { name, exact: true }), { steps: 10 });
        await d.pause(180);
      }
      await d.pause(600);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-1. 연락처 → 의견 요청 → 집계 → 리마인드 (기능 연계)
  {
    id: 'c-flow',
    kind: 'card',
    duration: 3000,
    card: { tone: 'light', kicker: '01  연락처에서 회신까지', title: '현행화한 연락처로 묻고,\n회신은 저절로 모입니다.' },
    narration: '첫째, 연락처에서 회신까지 한 흐름입니다.',
  },
  {
    id: 'flow-contacts',
    kind: 'app',
    narration:
      '기관 담당자가 로그인하면 6개월이 지난 연락처를 바로 묻습니다. 변동 없음 한 번이면 끝. 정기 변경 주간에는 현행화 완료를 누르면 되고, 바뀐 내역은 이력으로 남습니다.',
    run: async (d, page) => {
      await d.switchSession(KIM);
      await page.goto('/account-info/home');
      const reconfirm = page.getByTestId('contact-reconfirm');
      await reconfirm.waitFor();
      await d.pause(500);
      await d.caption('6개월이 지나면 로그인할 때 묻습니다. <b>변동 없음</b> 한 번이면 끝.');
      await d.pause(1100);
      await d.click(reconfirm.getByRole('button', { name: '변동 없음' }));
      await reconfirm.getByText('확인 완료').waitFor();
      await d.pause(700);
      await d.click(reconfirm.getByRole('button', { name: '닫기' }));
      await d.pause(300);
      await d.click(page.getByRole('navigation').getByRole('link', { name: '연락처', exact: true }));
      const table = page.getByTestId('contact-table');
      await table.getByText('남궁민').waitFor();
      await d.caption('정기 변경 주간에는 <b>현행화 완료</b>를 누르면 됩니다.');
      const banner = page.getByTestId('campaign-banner');
      await d.click(banner.getByRole('button', { name: '현행화 완료' }));
      await banner.getByText(/KB국민은행 현행화 완료/).waitFor();
      await d.pause(900);
      await d.caption('자기 기관만 보고 고칩니다. 바뀐 내역은 <b>이력으로</b> 남습니다.');
      await d.click(table.getByRole('button', { name: 'KB국민은행 남궁민' }));
      const history = page.getByTestId('contact-detail').getByTestId('change-history');
      await history.waitFor();
      await d.pause(300);
      await d.zoomTo(history, 1.5);
      await d.pause(1700);
      await d.unzoom();
      await d.pause(400);
      await d.caption(null);
      await d.pause(250);
    },
  },
  {
    id: 'flow-editor',
    kind: 'app',
    narration:
      '결제원 담당자가 의견 요청을 만들 때, 대상 기관과 회신 담당자는 방금 현행화된 연락처 명부에서 고릅니다. 세부업무 하나를 누르면 그 담당자들이 그대로 들어옵니다. 다시 묻지 않습니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await page.goto('/account-info/opinions/new');
      const chips = page.getByRole('group', { name: '세부업무로 고르기' });
      await chips.waitFor();
      await d.pause(400);
      await chips.scrollIntoViewIfNeeded();
      await d.pause(500);
      await d.caption('의견 요청의 대상은 <b>방금 현행화한 연락처</b>에서 고릅니다.');
      await d.pause(900);
      await d.click(chips.getByText('내 계좌 한눈에', { exact: true }));
      const checked = page.getByRole('checkbox', { name: '신한은행 박지훈' });
      await checked.waitFor();
      await d.centerOn(checked, 0.62);
      await d.caption('세부업무 하나를 누르면 담당자가 <b>그대로</b> 들어옵니다. 다시 묻지 않습니다.');
      await d.zoomBetween(chips, checked, 1.3);
      await d.pause(2400);
      await d.unzoom();
      await d.pause(300);
      await d.moveTo(page.getByTestId('target-summary'));
      await d.pause(1100);
      await d.caption(null);
      await d.pause(250);
    },
  },
  {
    id: 'flow-branch',
    kind: 'app',
    narration:
      '질문은 구글폼처럼 답에 따라 갈라집니다. 반영 가능이면 계획을, 어려우면 이유를 묻습니다. 기관 담당자는 자기 답에 맞는 질문만 보고, 웹에서 바로 제출합니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
      const questions = page.getByTestId('question-list');
      await questions.waitFor();
      await page.getByRole('heading', { name: BRANCHED_OPINION }).waitFor();
      await d.pause(500);
      await d.caption('질문은 구글폼처럼 <b>답에 따라 갈라집니다</b>.');
      await d.zoomTo(questions, 1.3);
      await d.pause(2300);
      await d.unzoom();
      await d.pause(400);

      await d.skipReconfirm();
      await d.switchSession(KIM);
      await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
      const reply = page.getByTestId('my-reply');
      await reply.waitFor();
      await reply.scrollIntoViewIfNeeded();
      await d.pause(500);
      await d.caption('기관은 <b>자기 답에 맞는 질문만</b> 보고, 웹에서 바로 제출합니다.');
      await d.click(reply.getByRole('radio', { name: '반영 어려움' }));
      await reply.getByText('섹션 3. 반영이 어려운 이유').waitFor();
      await d.pause(700);
      await d.type(reply.getByRole('textbox', { name: /반영하기 어려운 이유/ }), '내년 상반기 전산 개편 중이라 하반기 반영을 제안합니다.', {
        delay: 26,
      });
      await d.pause(300);
      await d.click(reply.getByRole('button', { name: '최종제출' }));
      await page.getByTestId('submitted-answers').waitFor();
      await d.pause(1100);
      await d.caption(null);
      await d.pause(250);
    },
  },
  {
    id: 'flow-collation',
    kind: 'app',
    narration:
      '결제원 담당자에게는 회신율과 질문별 답이 자동으로 모입니다. 건너뛴 질문도 보입니다. 미회신 기관에는 메일을 미리 보고 리마인드를 보냅니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
      await page.getByRole('heading', { name: BRANCHED_OPINION }).waitFor();
      await d.click(page.getByRole('tab', { name: '회신 현황' }));
      await page.getByTestId('reply-rate').waitFor();
      await d.caption('회신율과 질문별 답이 <b>자동으로</b> 모입니다. 건너뛴 질문도 보입니다.');
      await d.zoomTo(page.getByTestId('reply-rate'), 1.3);
      await d.pause(1800);
      await d.unzoom();
      await d.pause(300);
      await d.moveTo(page.getByTestId('question-summary'));
      await d.pause(700);
      await d.caption('미회신 기관에는 메일을 <b>미리 보고</b> 리마인드.');
      await d.click(page.getByRole('button', { name: '미회신 기관 리마인드' }));
      const remind = page.getByRole('dialog', { name: '리마인드 메일 보내기' });
      await remind.waitFor();
      await d.pause(700);
      await d.click(remind.getByRole('button', { name: '다음: 메일 확인' }));
      const check = page.getByRole('dialog', { name: '리마인드 메일 확인' });
      await check.getByTestId('mail-preview').waitFor();
      await d.pause(1400);
      await d.click(check.getByRole('button', { name: /곳에 보내기$/ }));
      await page.getByTestId('remind-sent').waitFor();
      await d.pause(1100);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-2. 기관 테스트
  {
    id: 'c-tests',
    kind: 'card',
    duration: 2900,
    card: { tone: 'light', kicker: '02  기관 테스트', title: '결과는 기관이, 의견은 결제원이,\n수행률은 저절로.' },
    narration: '둘째, 기관 테스트.',
  },
  {
    id: 'tests-org',
    kind: 'app',
    narration:
      '개발 건마다 테스트 페이지가 열립니다. 기관 담당자는 요구사항마다 결과를 고르고 저장합니다. 수행률은 바로 집계되고, 결제원 담당자에게 매터모스트로 알립니다.',
    run: async (d, page) => {
      await d.skipReconfirm();
      await d.switchSession(KIM);
      await page.goto('/account-info/tests/1');
      await page.getByRole('heading', { name: SEEDED_TEST }).waitFor();
      await d.pause(600);
      await d.caption('요구사항마다 <b>정상·오류</b>를 고르고');
      const item = page.getByTestId('item-005');
      await item.scrollIntoViewIfNeeded();
      await d.pause(700);
      await d.click(item.getByText('정상', { exact: true }));
      await d.pause(400);
      await d.caption('저장하면 <b>수행률이 바로</b> 집계됩니다.');
      await d.click(page.getByRole('button', { name: /결과 저장/ }));
      await d.pause(600);
      await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
      await d.pause(600);
      await d.zoomBetween(page.getByRole('heading', { name: SEEDED_TEST }), page.getByTestId('plan-progress'), 1.3);
      await d.pause(1900);
      await d.unzoom();
      await d.pause(400);
      await d.caption(null);
      await d.pause(250);
    },
  },
  {
    id: 'tests-kftc',
    kind: 'app',
    narration:
      '결제원 담당자는 기관별 현황을 한눈에 보고, 요구사항마다 의견과 미니 수행률을 남깁니다. 기관이 다시 고치면 "다시 확인"이 붙고, 같은 칸에서 답글로 이어집니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await page.goto('/account-info/tests/1');
      const targets = page.getByTestId('target-table');
      await targets.waitFor();
      await d.pause(500);
      await d.caption('결제원은 <b>기관별 현황</b>을 한눈에.');
      await d.zoomTo(targets, 1.25);
      await d.pause(1700);
      await d.unzoom();
      await d.pause(300);
      const item = page.getByTestId('item-004');
      await item.scrollIntoViewIfNeeded();
      await d.pause(500);
      await d.caption('요구사항마다 <b>의견과 미니 수행률</b>을 남깁니다.');
      await d.click(item.getByRole('button', { name: '004 의견' }));
      const dialog = page.getByRole('dialog');
      await dialog.getByRole('textbox', { name: '미니 수행률' }).waitFor();
      await d.type(dialog.getByRole('textbox', { name: '미니 수행률' }), '80');
      await d.type(dialog.getByRole('textbox', { name: '특이사항' }), '응답코드 매핑은 확인했습니다. 9999 건만 다시 봐 주세요.', {
        delay: 20,
      });
      await d.pause(300);
      await d.click(dialog.getByRole('button', { name: '저장' }));
      await item.getByTestId('item-feedback').waitFor();
      await d.pause(500);
      await d.caption('기관이 고치면 <b>다시 확인</b>, 같은 칸에서 <b>답글</b>로 이어집니다.');
      const third = page.getByTestId('item-003');
      await third.scrollIntoViewIfNeeded();
      await d.click(third.getByRole('button', { name: '003 답글' }));
      await third.getByTestId('item-comments').waitFor();
      await d.pause(1700);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-3. 일시중단
  {
    id: 'c-suspensions',
    kind: 'card',
    duration: 2900,
    card: { tone: 'light', kicker: '03  일시중단', title: '표준 양식으로 등록,\n확인부터 인수인계까지.' },
    narration: '셋째, 일시중단.',
  },
  {
    id: 'suspensions',
    kind: 'app',
    narration:
      '기관이 표준 양식으로 등록하면 검토 대기가 됩니다. 결제원 담당자는 확인을 시작하고, 조치와 인수인계 사항을 써서 확정합니다. 확정된 일정은 전 영업일에 모아 운영·관제 매터모스트 채널로 보냅니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await page.goto('/account-info/suspensions?period=UPCOMING');
      const table = page.getByTestId('suspension-table');
      await table.waitFor();
      await d.pause(500);
      await d.caption('기관이 양식으로 등록하면 <b>검토 대기</b>.');
      await d.pause(700);
      await d.click(table.getByRole('button').filter({ hasText: '신한은행' }).first());
      await page.getByRole('heading', { name: '신한은행 일시중단' }).waitFor();
      await d.pause(400);
      await d.click(page.getByRole('button', { name: '확인 시작' }));
      await d.click(page.getByRole('dialog', { name: '확인 시작' }).getByRole('button', { name: '확인 시작' }));
      await page.getByTestId('suspension-status').getByText('검토 중').waitFor();
      await d.caption('<b>확인 시작</b>, 조치 쓰고 <b>확정</b>. 인수인계가 담당자마다 남습니다.');
      await d.pause(500);
      await d.click(page.getByRole('button', { name: '조치 쓰고 확정' }));
      const handover = page.getByRole('dialog', { name: '조치 쓰고 확정' });
      await d.type(handover.getByRole('textbox', { name: '조치 및 인수인계 사항' }), '23:00 신한은행 연계 차단, 재개 연락 받으면 해제 후 상황실 공유.', {
        delay: 18,
      });
      await d.pause(250);
      await d.click(handover.getByRole('button', { name: '확정하기' }));
      await page.getByTestId('suspension-status').getByText('확정').waitFor();
      await d.pause(900);
      await d.click(page.getByRole('navigation').getByRole('link', { name: '일시중단', exact: true }));
      await page.getByRole('button', { name: '수동으로 보내기' }).waitFor();
      await d.caption('전 영업일 확정분은 매일 16시, <b>운영·관제 채널</b>로.');
      await d.click(page.getByRole('button', { name: '수동으로 보내기' }));
      await d.click(page.getByRole('dialog', { name: '전 영업일 취합 수동으로 보내기' }).getByRole('button', { name: '보내기' }));
      await page.getByRole('dialog', { name: '전 영업일 취합을 보냈습니다' }).getByTestId('digest-body').waitFor();
      await d.pause(2000);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-4. 소통 채널
  {
    id: 'c-chat',
    kind: 'card',
    duration: 2900,
    card: { tone: 'light', kicker: '04  소통 채널', title: '@기관을 부르면,\n기관이 확인합니다.' },
    narration: '넷째, 소통 채널.',
  },
  {
    id: 'chat',
    kind: 'app',
    narration:
      '업무마다 단체방이 저절로 열립니다. 기관을 불러 공지하면, 기관이 확인을 누른 것이 기관별로 보입니다. 전화로 하나씩 확인하던 일입니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await page.goto('/account-info/chat');
      const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
      const groupRoom = rooms.getByRole('link', { name: /계좌통합관리 단체방/ });
      await groupRoom.waitFor();
      await d.pause(400);
      await d.caption('업무마다 <b>단체방</b>이 저절로. <b>@기관</b>을 부르면 기관이 <b>확인</b>을 누릅니다.');
      await d.click(groupRoom);
      const messages = page.getByTestId('chat-messages');
      await messages.getByText('@결제원', { exact: true }).waitFor();
      await d.pause(500);
      await d.zoomTo(messages.getByTestId('chat-ack').first(), 1.4);
      await d.pause(1500);
      await d.unzoom();
      await d.pause(300);
      await d.caption(null);
      const input = page.getByRole('textbox', { name: '메시지 입력' });
      await d.click(input);
      await page.keyboard.type('@KB', { delay: 70 });
      await d.click(page.getByRole('option', { name: '@KB국민은행' }));
      await page.keyboard.type(' 11/3 응답코드 변경분 반영 확인 부탁드립니다.', { delay: 30 });
      await d.pause(250);
      await d.click(page.getByRole('button', { name: '보내기' }));
      await messages.getByTestId('chat-message').last().getByText('@KB국민은행', { exact: true }).waitFor();
      await d.pause(1200);
    },
  },

  // ───────────── 4. 기대효과 · 완성도 · AI
  {
    id: 'effect',
    kind: 'card',
    duration: 5200,
    card: {
      tone: 'dark',
      kicker: '업무 하나에 해마다 305시간 · 사업 계층별 분류체계 소분류 94개 업무로 넓히면',
      stat: { value: 28670, unit: '시간', label: '해마다, 우리 원 전체에서 절감되는 시간' },
    },
    narration: '업무 하나에 해마다 305시간. 사업 계층별 분류체계 소분류 94개 업무로 넓히면, 해마다 2만 8천 670시간입니다.',
  },
  {
    id: 'tech',
    kind: 'card',
    duration: 4000,
    card: {
      tone: 'dark',
      title: 'app.jar 하나.\n외부 연결 없이, 내부망에서 바로.',
      subtitle: 'Java 17 · Spring Boot · 설치 없음 · 테스트 595개',
    },
    narration: '설치 없이 app.jar 하나로 뜨고, 외부 연결 없이 내부망에서 바로 씁니다. 테스트 595개가 매 커밋마다 돕니다.',
  },
  {
    id: 'ai',
    kind: 'card',
    duration: 4200,
    card: {
      tone: 'light',
      title: 'Claude와 함께 만들었습니다.',
      subtitle: '요구사항 → 설계 → 코드 → 테스트 → 리뷰 · 3명 · 12일 · PR 60개',
    },
    narration: '요구사항 정리부터 설계, 코드, 테스트, 리뷰까지 Claude와 함께, 3명이 12일에 만들었습니다.',
  },
  {
    id: 'outro',
    kind: 'card',
    duration: 4800,
    card: { tone: 'light', image: { src: '/brand/logo.png', width: 360 }, title: '확인은 기관이,\n관리는 결제원이.' },
    narration: '연결이. 확인은 기관이, 관리는 결제원이.',
  },
];
