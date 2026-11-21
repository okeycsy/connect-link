import type { Chapter } from './types.ts';
import { AT_MEETING_NOTICE, AT_SLUG, AT_SURVEY, asLee, asMoon, atIds, look } from './at-common.ts';

/**
 * biz 업무팀: 회의 개최 공지 → 기관이 공지에서 바로 참석 회신 → 회신 현황·리마인드 → (회의) → 섹션이 나뉜 의견조사 →
 * 기관 회신(답에 따라 질문이 바뀜) → 취합표·질문별 집계·엑셀 → 요건 확정.
 */
export const atBiz: Chapter = {
  id: 'biz',
  title: '업무팀 — 회의 회신과 의견조사',
  scenes: [
    {
      id: 'at-biz-notice',
      kind: 'app',
      summary: '결제원(문서준) → 공지·자료실 → 고정 글 "…실무 회의 개최 안내" → 글을 열어 이어 둔 "회신 요청" 칸',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/notices`);
        const list = page.getByTestId('notice-list');
        const item = list.getByText(AT_MEETING_NOTICE);
        await item.waitFor();
        await d.pause(500);
        await look(d, item, 1500);
        await d.click(item);
        const linked = page.getByTestId('notice-opinion');
        await linked.waitFor();
        await d.pause(300);
        await look(d, linked, 2300);
      },
    },
    {
      id: 'at-biz-attend',
      kind: 'app',
      summary: '신한은행(이준호) → 같은 공지 → "회신하기" → "참석" · "이준호 과장" 입력 → 최종제출',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/notices?id=${atIds().noticeId}`);
        const linked = page.getByTestId('notice-opinion');
        await linked.waitFor();
        await d.pause(500);
        await look(d, linked, 1100);
        await d.clickUntil(page.getByRole('link', { name: '회신하기' }), page.getByText('회의에 참석하십니까?'));
        const reply = page.getByTestId('my-reply');
        await reply.waitFor();
        await look(d, reply);
        await d.click(reply.getByRole('radio', { name: '참석', exact: true }));
        await d.type(reply.getByRole('textbox', { name: /참석자 이름과 직위/ }), '이준호 과장', { delay: 30 });
        await d.pause(200);
        await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'));
        await d.pause(900);
      },
    },
    {
      id: 'at-biz-status',
      kind: 'app',
      summary: '결제원(문서준) → 참석 회신 요청의 "회신 현황" → 제출 기관 4/6 → 미회신 기관 리마인드 → 메일 미리보기까지 보이고 닫음',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/opinions/${atIds().meetingRequestId}?tab=replies`);
        const count = page.getByTestId('replied-count');
        await count.waitFor();
        await d.pause(400);
        await look(d, [count, page.getByTestId('reply-rate'), page.getByTestId('not-replied-names')], 1400);
        const remind = page.getByRole('dialog', { name: '리마인드 메일 보내기' });
        await d.clickUntil(page.getByRole('button', { name: '미회신 기관 리마인드' }), remind);
        const check = page.getByRole('dialog', { name: '리마인드 메일 확인' });
        await d.clickUntil(remind.getByRole('button', { name: '다음: 메일 확인' }), check.getByTestId('mail-preview'));
        await d.pause(200);
        await look(d, check, 1600);
        await page.keyboard.press('Escape');
        await d.pause(300);
      },
    },
    {
      id: 'at-biz-meeting-done',
      kind: 'card',
      duration: 2800,
      card: { tone: 'light', title: '회의를 마쳤습니다.\n이제 기관별 의견을 묻습니다.', titleSize: 64 },
    },
    {
      id: 'at-biz-survey',
      kind: 'app',
      summary: '결제원(문서준) → 의견조사 요청 내용 → 질문 5개와 섹션 이동(가능 → 섹션 2, 어려움 → 섹션 3) → 대상 기관별 회신 담당자(세부업무)',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/opinions/${atIds().surveyId}`);
        await page.getByRole('heading', { name: AT_SURVEY }).waitFor();
        const questions = page.getByTestId('question-list');
        await questions.waitFor();
        await d.pause(300);
        await look(d, questions.getByText(/가능 → 섹션 2/), 1800);
        // 대상은 연락처에서 세부업무로 골라 담은 회신 담당자다.
        const targets = page.getByTestId('target-table');
        await look(d, targets.locator('tr').filter({ hasText: '신한은행' }), 1000);
        await look(d, targets.locator('tr').filter({ hasText: '우체국' }), 1200);
      },
    },
    {
      id: 'at-biz-survey-reply',
      kind: 'app',
      summary:
        '신한은행(이준호) → 의견조사 회신 → "가능"(섹션 2) → "어려움"으로 바꾸면 섹션 3 으로 → 다시 "가능" → 2분기, 사유코드 3개, "약 2개월" → 최종제출',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/opinions/${atIds().surveyId}`);
        const reply = page.getByTestId('my-reply');
        await reply.waitFor();
        await d.pause(300);
        await look(d, reply);
        await d.click(reply.getByRole('radio', { name: '가능', exact: true }));
        await reply.getByText('섹션 2. 적용 계획').waitFor();
        await d.pause(900);
        await d.click(reply.getByRole('radio', { name: '어려움', exact: true }));
        await reply.getByText('섹션 3. 어려운 이유').waitFor();
        await d.pause(1300);
        await d.click(reply.getByRole('radio', { name: '가능', exact: true }));
        await reply.getByText('섹션 2. 적용 계획').waitFor();
        await d.pause(300);
        await d.click(reply.getByRole('radio', { name: '2분기', exact: true }));
        for (const code of ['미납 잔액 있음', '기관에서 직접 해지 필요', '출금계좌 해지됨']) {
          await d.click(reply.getByRole('checkbox', { name: code, exact: true }), { settle: 80 });
        }
        await d.type(reply.getByRole('textbox', { name: /개발에 필요한 기간/ }), '약 2개월', { delay: 40 });
        await d.pause(200);
        await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'));
        await d.pause(700);
      },
    },
    {
      id: 'at-biz-collation',
      kind: 'app',
      summary: '결제원(문서준) → 의견조사 "취합표"(우체국의 건너뜀 칸) → "회신 현황"의 질문별 집계 → 취합표 "엑셀 다운로드"',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/opinions/${atIds().surveyId}?tab=collation`);
        const table = page.getByTestId('collation-table');
        await table.waitFor();
        await d.pause(400);
        await look(d, table, 1800);
        await d.click(page.getByRole('tab', { name: '회신 현황' }));
        const summary = page.getByTestId('question-summary');
        await summary.waitFor();
        await d.pause(200);
        await look(d, summary, 1500);
        await d.click(page.getByRole('tab', { name: '취합표' }));
        const excel = page.getByRole('button', { name: '엑셀 다운로드' });
        await excel.waitFor();
        await look(d, excel, 1400);
      },
    },
    {
      id: 'at-biz-decided',
      kind: 'card',
      duration: 3400,
      card: { tone: 'light', kicker: '요건 확정', title: '당일 2회(13·18시) 회신 · 불가 사유코드 3종 · 2분기 시행', titleSize: 56 },
    },
  ],
};
