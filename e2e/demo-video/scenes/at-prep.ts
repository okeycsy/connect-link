import type { Chapter } from './types.ts';
import { AT_SLUG, asMoon, look } from './at-common.ts';

/** prep 준비: 과제를 시작하기 전, 현행화된 연락처에서 기관·세부업무별 담당자를 바로 찾는다. */
export const atPrep: Chapter = {
  id: 'prep',
  title: '준비 — 현행화된 연락처',
  scenes: [
    {
      id: 'at-prep',
      kind: 'app',
      summary:
        '결제원(문서준) → 자동이체통합관리 연락처 → 기관별 담당자 표(세부업무·최근 확인일) → KB국민은행 남도윤 상세 → 변경 이력(이전 담당자 백승호, 휴대폰은 가림) → 닫기 → "정기 변경 주간" → 6개월 주기·미완료 기관 리마인드 안내',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/contacts`);
        const table = page.getByTestId('contact-table');
        await table.getByText('장서윤').waitFor();
        await d.pause(900);
        // 기관별로 담당자를 훑고, 6개월이 지난 "확인 필요" 담당자에 커서를 둔다.
        await look(d, table.getByRole('button', { name: 'KB국민은행 장서윤' }), 900);
        await look(d, table.getByRole('button', { name: '신한은행 이준호' }), 900);
        await look(d, table.getByText('확인 필요', { exact: true }), 1800);
        await d.click(table.getByRole('button', { name: 'KB국민은행 남도윤' }));
        const detail = page.getByTestId('contact-detail');
        const history = detail.getByTestId('change-history');
        await history.waitFor();
        await d.pause(700);
        await look(d, history, 3600);
        await page.keyboard.press('Escape');
        await detail.waitFor({ state: 'hidden' });
        await d.pause(400);
        // 정기 변경: 반기마다 스스로 요청하고 미완료 기관만 리마인드한다.
        await d.click(page.getByRole('link', { name: '정기 변경 주간' }));
        const schedule = page.getByTestId('campaign-schedule');
        await schedule.waitFor();
        await look(d, schedule, 3200);
      },
    },
  ],
};
