import type { Page } from '@playwright/test';

import type { CardSpec } from './cards.ts';
import type { Director } from './helpers.ts';

/**
 * 시연영상 장면. 순서대로 녹화해 이어 붙인다(record.ts → compose.ts).
 * 문구·순서는 시나리오 문서(프로젝트 파일 demo-video/scenario-v3.md)의 장면표와 같다. 바꾸면 둘 다 고친다.
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
/** 소통 채널 장면에서 결제원이 보내는 긴급 확인 요청. */
const URGENT_MESSAGE = '내 계좌 한눈에 조회 전문 응답 지연 장애가 발생했습니다. 귀 기관 영향 여부를 긴급히 확인해 주세요.';
/** 참석 회신 의견 요청을 이어 둔 회의 개최 안내 공지. */
const MEETING_NOTICE = '계좌통합관리업무 전문 변경 회의 개최 안내';

export const SCENES: Scene[] = [
  // ───────────── 1. 문제 (업무 관련성)
  {
    id: 'p1',
    kind: 'card',
    duration: 3400,
    card: {
      tone: 'light',
      kicker: '결제원 업무의 중심',
      title: '참가기관과의 소통과 취합,\n우리 원 업무의 핵심입니다.',
      subtitle: '담당자 변경 · 의견 회신 · 기관 테스트 · 일시중단 일정, 매일 수십 번',
    },
    narration: '참가기관과의 소통과 취합은 우리 원 업무의 핵심입니다. 담당자 변경, 의견 회신, 기관 테스트, 일시중단 일정까지, 매일 수십 번 오갑니다.',
  },
  {
    id: 'p2',
    kind: 'card',
    duration: 3600,
    card: {
      tone: 'light',
      title: '전화로 묻고, 메일로 받고,\n채널마다 흩어져 관리됩니다.',
      subtitle: '전화 · 메일 · 엑셀 · 메신저 · 공문 — 담당자마다, 기관마다 따로',
    },
    narration: '그런데 이 소통은 전화로 묻고, 메일로 받고, 엑셀에 옮기며 여러 채널에 흩어져 관리됩니다. 담당자가 바뀌면 처음부터 다시 찾습니다.',
  },
  {
    id: 'p3',
    kind: 'card',
    duration: 4100,
    card: {
      tone: 'light',
      stat: { value: 305, unit: '시간', label: '업무 하나에서, 해마다 메일을 하나하나 열고 이를 내부망에서 취합하는 데 걸리는 시간' },
    },
    narration: '업무 하나에서만, 해마다 305시간이 메일을 하나하나 열고 내부망에서 취합하는 데 들어갑니다.',
  },

  // ───────────── 2. 소개 (확산 가능성)
  {
    id: 'intro',
    kind: 'card',
    duration: 3800,
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
      '결제원 담당자 홍성민이 회사 이메일로 로그인합니다. 로그인하면 지금 할 일만 보입니다. 어느 업무든 같은 메뉴, 같은 구조라 94개 업무로 그대로 넓힐 수 있습니다.',
    run: async (d, page) => {
      await page.goto('/login');
      const demo = page.getByTestId('demo-accounts');
      await demo.waitFor();
      await d.pause(600);
      await d.caption('결제원도, 참가기관도 같은 입구. <b>회사 이메일 하나</b>로.');
      await d.pause(700);
      const hong = page.getByRole('button', { name: /^홍성민 .*로 로그인/ });
      await hong.scrollIntoViewIfNeeded();
      await d.caption('<b>결제원 담당자 홍성민</b>으로 로그인합니다.');
      await d.zoomTo(hong, 1.3);
      await d.pause(1000);
      await d.unzoom();
      await d.pause(500);
      await d.click(hong);
      await page.waitForURL(/\/home$/);
      await page.getByRole('heading', { name: /안녕하세요/ }).waitFor();
      await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
      await d.pause(400);
      await d.caption('로그인하면 <b>지금 할 일</b>만 보입니다.');
      await d.zoomTo(page.getByText('다가오는 일시중단', { exact: true }), 1.3);
      await d.pause(1400);
      await d.unzoom();
      await d.pause(400);
      await d.caption('어느 업무든 <b>같은 구조</b>. 94개 업무까지 그대로.');
      const nav = page.getByRole('navigation');
      for (const name of ['연락처', '의견 요청', '기관 테스트', '일시중단', '소통 채널']) {
        await d.moveTo(nav.getByRole('link', { name, exact: true }), { steps: 8 });
        await d.pause(100);
      }
      await d.pause(500);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-1. 연락처 → 의견 요청 → 집계 → 리마인드 (기능 연계)
  {
    id: 'c-flow',
    kind: 'card',
    duration: 2800,
    card: { tone: 'light', kicker: '01  연락처에서 회신까지', title: '현행화한 연락처로 묻고,\n회신은 저절로 모입니다.' },
    narration: '첫째, 연락처에서 회신까지 한 흐름입니다.',
  },
  {
    id: 'flow-contacts',
    kind: 'app',
    narration:
      '이번에는 KB국민은행 담당자 김민준입니다. 로그인하면 6개월이 지난 연락처를 바로 묻습니다. 변동 없음 한 번이면 끝. 정기 변경 주간에는 현행화 완료를 누르면 되고, 바뀐 내역은 이력으로 남습니다.',
    run: async (d, page) => {
      await d.switchSession(KIM);
      await d.who('org', 'KB국민은행', '김민준 대리 · 참가기관 담당자');
      await page.goto('/account-info/home');
      const reconfirm = page.getByTestId('contact-reconfirm');
      await reconfirm.waitFor();
      await d.pause(400);
      await d.caption('<b>KB국민은행 담당자 김민준</b>으로 로그인하면, 6개월 지난 담당자를 바로 묻습니다.');
      await d.pause(1200);
      await d.caption('<b>변동 없음</b> 한 번이면 끝.');
      await d.pause(500);
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
      await d.pause(1400);
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
      '다시 결제원 담당자입니다. 의견 요청을 만들 때, 대상 기관과 회신 담당자는 방금 현행화된 연락처 명부에서 고릅니다. 세부 업무 하나를 누르면 해당 업무 담당자가 바로 수신자로 지정됩니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
      await page.goto('/account-info/opinions/new');
      const chips = page.getByRole('group', { name: '세부업무로 고르기' });
      await chips.waitFor();
      await d.pause(400);
      await chips.scrollIntoViewIfNeeded();
      await d.pause(500);
      await d.caption('다시 결제원 담당자. 의견 요청의 대상은 <b>방금 현행화한 연락처</b>에서 고릅니다.');
      await d.pause(900);
      await d.click(chips.getByText('내 계좌 한눈에', { exact: true }));
      const checked = page.getByRole('checkbox', { name: '신한은행 박지훈' });
      await checked.waitFor();
      await d.centerOn(checked, 0.62);
      await d.caption('세부 업무 하나를 누르면 해당 업무 담당자가 <b>바로 수신자로</b> 지정됩니다.');
      await d.zoomBetween(chips, checked, 1.3);
      await d.pause(2000);
      await d.unzoom();
      await d.pause(300);
      await d.moveTo(page.getByTestId('target-summary'));
      await d.pause(900);
      await d.caption(null);
      await d.pause(250);
    },
  },
  {
    id: 'flow-branch',
    kind: 'app',
    narration:
      '질문은 구글폼처럼 답에 따라 다음 질문이 달라집니다. 반영 가능이면 계획을, 어려우면 이유를 묻습니다. 기관 담당자가 답을 고르면 그에 맞는 질문만 이어져, 정확한 답을 받습니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
      await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
      const questions = page.getByTestId('question-list');
      await questions.waitFor();
      await page.getByRole('heading', { name: BRANCHED_OPINION }).waitFor();
      await d.pause(400);
      await d.caption('구글폼처럼, 답에 따라 <b>다음 질문이 달라집니다</b>. 반영 가능이면 계획을, 어려우면 이유를.');
      await d.zoomTo(questions, 1.3);
      await d.pause(2200);
      await d.unzoom();
      await d.pause(400);

      await d.skipReconfirm();
      await d.switchSession(KIM);
      await d.who('org', 'KB국민은행', '김민준 대리 · 참가기관 담당자');
      await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
      const reply = page.getByTestId('my-reply');
      await reply.waitFor();
      await d.centerOn(reply, 0.4);
      await d.caption('KB국민은행 담당자가 답을 고르면, <b>그 답에 맞는 질문만</b> 이어집니다.');
      await d.click(reply.getByRole('radio', { name: '반영 가능' }));
      await reply.getByText('섹션 2. 반영 계획').waitFor();
      await d.pause(1000);
      await d.click(reply.getByRole('radio', { name: '반영 어려움' }));
      await reply.getByText('섹션 3. 반영이 어려운 이유').waitFor();
      await d.pause(500);
      await d.caption('해당 없는 질문은 보이지 않아 <b>정확한 답</b>을 받습니다. 웹에서 바로 제출.');
      await d.type(reply.getByRole('textbox', { name: /반영하기 어려운 이유/ }), '내년 상반기 전산 개편 중이라 하반기 반영을 제안합니다.', {
        delay: 18,
      });
      await d.pause(300);
      await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'));
      await d.pause(900);
      await d.caption(null);
      await d.pause(250);
    },
  },
  {
    id: 'notice-meeting',
    kind: 'app',
    narration:
      '공지·자료실도 같은 흐름입니다. 회의 개최 안내를 보다가 회신하기를 누르면, 참석 여부와 참석자를 바로 답합니다. 메일로 회신서를 보내지 않습니다.',
    run: async (d, page) => {
      await d.skipReconfirm();
      await d.switchSession(KIM);
      await d.who('org', 'KB국민은행', '김민준 대리 · 참가기관 담당자');
      await page.goto('/account-info/notices');
      const list = page.getByTestId('notice-list');
      await list.getByText(MEETING_NOTICE).waitFor();
      await d.pause(500);
      await d.caption('공지·자료실에서 <b>회의 개최 안내</b>를 보다가,');
      await d.click(list.getByText(MEETING_NOTICE));
      const linked = page.getByTestId('notice-opinion');
      await linked.waitFor();
      await d.pause(700);
      await d.caption('글에 이어 둔 <b>회신하기</b>로 참석 여부를 바로 답합니다.');
      await d.clickUntil(page.getByRole('link', { name: '회신하기' }), page.getByText('회의에 참석하십니까?'));
      const reply = page.getByTestId('my-reply');
      await reply.waitFor();
      await d.centerOn(reply, 0.4);
      await d.click(reply.getByRole('radio', { name: '참석', exact: true }));
      await d.pause(300);
      await d.type(reply.getByRole('textbox', { name: /참석자 이름과 직위/ }), '김민준 대리, 남궁민 과장', { delay: 22 });
      await d.pause(250);
      await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'));
      await d.pause(900);
      await d.caption(null);
      await d.pause(250);
    },
  },
  {
    id: 'flow-collation',
    kind: 'app',
    narration:
      '결제원 담당자에게는 질문별 답이 자동으로 모입니다. 건너뛴 질문도 보입니다. 미회신 기관에는 메일을 미리 보고 리마인드를 보냅니다. 보낸 메일은 발송함에 쌓이고, 리마인드와 알림 메일이 자동으로 나갑니다. 메일을 하나하나 열어 취합하던 일이 사라집니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
      await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
      await page.getByRole('heading', { name: BRANCHED_OPINION }).waitFor();
      await d.click(page.getByRole('tab', { name: '회신 현황' }));
      const summary = page.getByTestId('question-summary');
      await summary.waitFor();
      await d.caption('결제원 담당자에게는 <b>질문별 답</b>이 자동으로 모입니다. 건너뛴 질문도 보입니다.');
      await d.centerOn(summary, 0.5);
      await d.zoomTo(summary, 1.3);
      await d.pause(2100);
      await d.unzoom();
      await d.pause(400);
      await d.caption('미회신 기관에는 메일을 <b>미리 보고</b> 리마인드.');
      const remind = page.getByRole('dialog', { name: '리마인드 메일 보내기' });
      await d.clickUntil(page.getByRole('button', { name: '미회신 기관 리마인드' }), remind);
      await d.pause(700);
      const check = page.getByRole('dialog', { name: '리마인드 메일 확인' });
      await d.clickUntil(remind.getByRole('button', { name: '다음: 메일 확인' }), check.getByTestId('mail-preview'));
      await d.pause(1000);
      await d.clickUntil(check.getByRole('button', { name: /곳에 보내기$/ }), page.getByTestId('remind-sent'));
      await d.pause(800);
      // 데모 발송함: 리마인드·알림 메일이 자동으로 나간 기록을 보여 준다.
      await d.caption('보낸 메일은 <b>발송함</b>에 쌓입니다. 리마인드·알림 메일이 <b>자동으로</b> 나갑니다.');
      await d.click(page.getByRole('button', { name: '데모 도구' }));
      await d.clickUntil(page.getByRole('menuitem', { name: '발송함' }), page.getByTestId('outbox-table'));
      const rows = page.getByTestId('outbox-table').locator('tbody tr');
      await rows.nth(2).waitFor();
      await d.pause(500);
      await d.zoomBetween(rows.nth(0), rows.nth(2), 1.3);
      await d.pause(1900);
      await d.unzoom();
      await d.pause(300);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-2. 기관 테스트
  {
    id: 'c-tests',
    kind: 'card',
    duration: 2600,
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
      await d.who('org', 'KB국민은행', '김민준 대리 · 참가기관 담당자');
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
      await d.pause(1600);
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
      await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
      await page.goto('/account-info/tests/1');
      const targets = page.getByTestId('target-table');
      await targets.waitFor();
      await d.pause(500);
      await d.caption('결제원은 <b>기관별 현황</b>을 한눈에.');
      await d.zoomTo(targets, 1.25);
      await d.pause(1200);
      await d.unzoom();
      await d.pause(300);
      const item = page.getByTestId('item-004');
      await item.scrollIntoViewIfNeeded();
      await d.pause(500);
      await d.caption('요구사항마다 <b>의견과 미니 수행률</b>을 남깁니다.');
      const dialog = page.getByRole('dialog');
      await d.clickUntil(item.getByRole('button', { name: '004 의견' }), dialog.getByRole('textbox', { name: '미니 수행률' }));
      await d.type(dialog.getByRole('textbox', { name: '미니 수행률' }), '80');
      await d.type(dialog.getByRole('textbox', { name: '특이사항' }), '응답코드 매핑은 확인했습니다. 9999 건만 다시 봐 주세요.', {
        delay: 16,
      });
      await d.pause(300);
      await d.clickUntil(dialog.getByRole('button', { name: '저장' }), item.getByTestId('item-feedback'));
      await d.pause(500);
      await d.caption('기관이 고치면 <b>다시 확인</b>, 같은 칸에서 <b>답글</b>로 이어집니다.');
      const third = page.getByTestId('item-003');
      await third.scrollIntoViewIfNeeded();
      await d.click(third.getByRole('button', { name: '003 답글' }));
      await third.getByTestId('item-comments').waitFor();
      await d.pause(1000);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-3. 일시중단
  {
    id: 'c-suspensions',
    kind: 'card',
    duration: 2600,
    card: { tone: 'light', kicker: '03  일시중단', title: '업무를 하나하나 골라 등록,\n확인부터 인수인계까지.' },
    narration: '셋째, 일시중단.',
  },
  {
    id: 'suspensions',
    kind: 'app',
    narration:
      '기관 담당자는 다른 기관의 확정 일정도 함께 보고, 전문 응답이 없을 때 참고합니다. 등록할 때는 중단되는 업무를 하나하나 골라 누락 없이, 정확하게 결제원에 알립니다. 결제원 담당자는 검토 대기 건을 확인하고, 조치와 인수인계 사항을 써서 확정합니다.',
    run: async (d, page) => {
      await d.skipReconfirm();
      await d.switchSession(KIM);
      await d.who('org', 'KB국민은행', '김민준 대리 · 참가기관 담당자');
      await page.goto('/account-info/suspensions');
      const others = page.getByTestId('other-schedules');
      await others.waitFor();
      await d.pause(400);
      await d.caption('기관 담당자 화면. <b>다른 기관의 확정 일정</b>도 함께 보여, 전문 응답이 없을 때 참고합니다.');
      await d.zoomTo(others, 1.3);
      await d.pause(1800);
      await d.unzoom();
      await d.pause(300);
      await d.click(page.getByRole('link', { name: '일시중단 등록' }));
      await page.getByRole('heading', { name: '일시중단 등록' }).waitFor();
      const works = page.getByTestId('suspension-businesses');
      await works.waitFor();
      await d.centerOn(works, 0.45);
      await d.caption('등록할 때 중단되는 업무를 <b>하나하나 골라</b> 누락 없이, 정확하게 알립니다.');
      await d.pause(600);
      await d.click(works.getByRole('checkbox', { name: '계좌통합관리', exact: true }));
      await d.pause(300);
      await d.click(works.getByRole('checkbox', { name: 'CMS', exact: true }));
      await page.getByTestId('suspension-businesses-count').getByText('고른 업무 2개').waitFor();
      await d.zoomTo(page.getByTestId('suspension-businesses-count'), 1.3);
      await d.pause(1200);
      await d.unzoom();
      await d.pause(300);

      await d.switchSession(HONG);
      await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
      await page.goto('/account-info/suspensions?period=UPCOMING');
      const table = page.getByTestId('suspension-table');
      await table.waitFor();
      await d.pause(400);
      await d.caption('결제원 담당자 화면. 기관이 등록하면 <b>검토 대기</b>.');
      await d.pause(600);
      await d.click(table.getByRole('button').filter({ hasText: '신한은행' }).first());
      await page.getByRole('heading', { name: '신한은행 일시중단' }).waitFor();
      await d.pause(400);
      const startDialog = page.getByRole('dialog', { name: '확인 시작' });
      await d.clickUntil(page.getByRole('button', { name: '확인 시작' }), startDialog);
      const status = page.getByTestId('suspension-status');
      await d.clickUntil(startDialog.getByRole('button', { name: '확인 시작' }), status.getByText('검토 중'));
      await d.caption('<b>확인 시작</b>, 조치 쓰고 <b>확정</b>. 인수인계가 담당자마다 남습니다.');
      await d.pause(500);
      const handover = page.getByRole('dialog', { name: '조치 쓰고 확정' });
      await d.clickUntil(page.getByRole('button', { name: '조치 쓰고 확정' }), handover);
      await d.type(handover.getByRole('textbox', { name: '조치 및 인수인계 사항' }), '23:00 신한은행 연계 차단, 재개 연락 받으면 해제 후 상황실 공유.', {
        delay: 14,
      });
      await d.pause(250);
      await d.clickUntil(handover.getByRole('button', { name: '확정하기' }), status.getByText('확정', { exact: true }));
      await d.pause(1100);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 3-4. 소통 채널
  {
    id: 'c-chat',
    kind: 'card',
    duration: 2600,
    card: { tone: 'light', kicker: '04  소통 채널', title: '장애가 나면 @기관을 부르고,\n기관이 확인합니다.' },
    narration: '넷째, 소통 채널.',
  },
  {
    id: 'chat',
    kind: 'app',
    narration:
      '장애가 나면 업무 단체방에서 기관을 불러 긴급 확인을 요청합니다. 기관 담당자가 확인을 누르면 메시지 아래에 기관 이름으로 남습니다. 전화로 하나씩 확인하던 일입니다.',
    run: async (d, page) => {
      await d.switchSession(HONG);
      await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
      await page.goto('/account-info/chat');
      const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
      const groupRoom = rooms.getByRole('link', { name: /계좌통합관리 단체방/ });
      await groupRoom.waitFor();
      await d.pause(300);
      await d.click(groupRoom);
      const messages = page.getByTestId('chat-messages');
      await messages.getByText('@결제원', { exact: true }).waitFor();
      await d.pause(300);
      await d.caption('장애가 나면 단체방에서 <b>@기관</b>을 불러 긴급 확인을 요청합니다.');
      const input = page.getByRole('textbox', { name: '메시지 입력' });
      await d.click(input);
      await page.keyboard.type('@KB', { delay: 50 });
      await d.click(page.getByRole('option', { name: '@KB국민은행' }));
      await page.keyboard.type(' ' + URGENT_MESSAGE, { delay: 18 });
      await d.pause(250);
      await d.click(page.getByRole('button', { name: '보내기' }));
      await messages.getByTestId('chat-message').last().getByText('@KB국민은행', { exact: true }).waitFor();
      await d.pause(900);
      await d.caption(null);

      await d.skipReconfirm();
      await d.switchSession(KIM);
      await d.who('org', 'KB국민은행', '김민준 대리 · 참가기관 담당자');
      await page.goto('/account-info/chat');
      await d.click(rooms.getByRole('link', { name: /계좌통합관리 단체방/ }));
      const urgent = messages.getByTestId('chat-message').filter({ hasText: URGENT_MESSAGE.slice(0, 12) }).last();
      await urgent.waitFor();
      await d.pause(300);
      await d.caption('KB국민은행 담당자가 <b>확인</b>을 누르면, 메시지 아래 <b>기관 이름</b>으로 남습니다.');
      await d.clickUntil(urgent.getByRole('button', { name: '확인', exact: true }), urgent.getByTestId('chat-ack'));
      await d.pause(300);
      await d.zoomTo(urgent.getByTestId('chat-ack'), 1.5);
      await d.pause(1900);
      await d.unzoom();
      await d.pause(500);
      await d.caption(null);
      await d.pause(250);
    },
  },

  // ───────────── 4. 기대효과 · 완성도 · AI
  {
    id: 'effect',
    kind: 'card',
    duration: 5000,
    card: {
      tone: 'light',
      kicker: '업무 하나에 해마다 305시간 · 사업 계층별 분류체계 소분류 94개 업무로 넓히면',
      stat: { value: 28670, unit: '시간 절감', label: '해마다, 우리 원 전체에서' },
    },
    narration: '업무 하나에 해마다 305시간. 소분류 94개 업무로 넓히면, 해마다 2만 8천 670시간을 절감합니다.',
  },
  {
    id: 'platform',
    kind: 'card',
    duration: 4200,
    card: {
      tone: 'light',
      kicker: '수기에서 플랫폼으로',
      title: '수기로 처리하던 소통을\n하나의 플랫폼으로.',
      subtitle: '표준 양식 · 자동 집계 · 변경 이력 · 알림 · 설치 없이 바로 도입',
    },
    narration: '수기와 메일로 처리하던 소통을 하나의 플랫폼으로 옮겼습니다. 표준 양식, 자동 집계, 변경 이력, 알림까지. 설치 없이 바로 도입할 수 있습니다.',
  },
  {
    id: 'ai',
    kind: 'card',
    duration: 4000,
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
    duration: 4600,
    card: {
      tone: 'light',
      image: { src: '/brand/logo.png', width: 340 },
      title: '더 긴밀한 소통,\n더 빠른 대응.',
      subtitle: '결제원과 참가기관을 잇는 연결이',
    },
    narration: '연결이. 결제원과 참가기관의 소통을 더 긴밀하게, 대응을 더 빠르게.',
  },
];
