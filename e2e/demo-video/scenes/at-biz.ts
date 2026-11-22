import type { Chapter } from './types.ts';
import { AT_SLUG, FAST, asLee, asMoon, atIds, look } from './at-common.ts';

/**
 * biz 업무팀(대본 v1.8, 약 26초): 공지에서 참석 회신 → 회신 현황·미제출 리마인드 → 의견조사 응답 분기 → 취합표.
 */
export const atBiz: Chapter = {
  id: 'biz',
  title: '업무팀 — 회의 회신과 의견조사',
  scenes: [
    {
      id: 'at-biz-attend',
      kind: 'app',
      summary: '신한은행(이준호) → 회의 개최 안내 공지 → "회신하기" → "참석" · "이준호 과장" → 최종제출',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/notices?id=${atIds().noticeId}`);
        await d.pause(1000);
        await page.getByTestId('notice-opinion').waitFor();
        await d.pause(500);
        await d.clickUntil(page.getByRole('link', { name: '회신하기' }), page.getByText('회의에 참석하십니까?'), FAST);
        const reply = page.getByTestId('my-reply');
        await d.click(reply.getByRole('radio', { name: '참석', exact: true }), FAST);
        await d.type(reply.getByRole('textbox', { name: /참석자 이름과 직위/ }), '이준호 과장', { delay: 25 });
        await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'), FAST);
        await d.pause(1800);
      },
    },
    {
      id: 'at-biz-status',
      kind: 'app',
      summary: '결제원(문서준) → 참석 회신의 "회신 현황"(제출 4/6) → 미회신 기관 리마인드 → 메일 미리보기',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/opinions/${atIds().meetingRequestId}?tab=replies`);
        await d.pause(1000);
        const count = page.getByTestId('replied-count');
        await count.waitFor();
        await look(d, count, 1500);
        const remind = page.getByRole('dialog', { name: '리마인드 메일 보내기' });
        await d.clickUntil(page.getByRole('button', { name: '미회신 기관 리마인드' }), remind, FAST);
        const check = page.getByRole('dialog', { name: '리마인드 메일 확인' });
        await d.clickUntil(remind.getByRole('button', { name: '다음: 메일 확인' }), check.getByTestId('mail-preview'), FAST);
        await look(d, check.getByTestId('mail-preview'), 2000);
      },
    },
    {
      id: 'at-biz-survey-reply',
      kind: 'app',
      summary: '신한은행(이준호) → 의견조사 회신 → "가능"(섹션 2) → "어려움"(섹션 3 으로 바뀜) → 다시 "가능" → 2분기 → 최종제출',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/opinions/${atIds().surveyId}`);
        await d.pause(1000);
        const reply = page.getByTestId('my-reply');
        await reply.waitFor();
        await d.centerOn(reply.getByText('섹션 1. 당일 2회 회신'), 0.25);
        await d.click(reply.getByRole('radio', { name: '가능', exact: true }), FAST);
        await reply.getByText('섹션 2. 적용 계획').waitFor();
        await d.pause(1000);
        await d.click(reply.getByRole('radio', { name: '어려움', exact: true }), FAST);
        await reply.getByText('섹션 3. 어려운 이유').waitFor();
        await d.pause(1500);
        await d.click(reply.getByRole('radio', { name: '가능', exact: true }), FAST);
        await reply.getByText('섹션 2. 적용 계획').waitFor();
        await d.click(reply.getByRole('radio', { name: '2분기', exact: true }), FAST);
        // 나머지 답은 화면 밖에서 빠르게 채운다(사유코드 3개, 개발 기간).
        for (const code of ['미납 잔액 있음', '기관에서 직접 해지 필요', '출금계좌 해지됨']) {
          await reply.getByRole('checkbox', { name: code, exact: true }).check();
        }
        await reply.getByRole('textbox', { name: /개발에 필요한 기간/ }).fill('약 2개월');
        await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'), FAST);
        await d.pause(1200);
      },
    },
    {
      id: 'at-biz-collation',
      kind: 'app',
      summary: '결제원(문서준) → 의견조사 "취합표"(기관별 답, 우체국의 건너뜀 칸)',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/opinions/${atIds().surveyId}?tab=collation`);
        await d.pause(1000);
        const table = page.getByTestId('collation-table');
        await table.waitFor();
        await d.pause(300);
        await look(d, table.locator('tr').filter({ hasText: '신한은행' }), 1500);
        await look(d, table.locator('tr').filter({ hasText: '우체국' }).getByText('건너뜀'), 2500);
      },
    },
  ],
};
