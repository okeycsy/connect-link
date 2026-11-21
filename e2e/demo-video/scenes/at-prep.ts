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
        '결제원(문서준) → 자동이체통합관리 연락처 → 기관별 담당자 표(세부업무·최근 확인일) → KB국민은행 장서윤 상세 → 변경 이력(이전 휴대폰은 가림) → 닫기 → "정기 변경 주간" 버튼',
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
        await d.click(table.getByRole('button', { name: 'KB국민은행 장서윤' }));
        const detail = page.getByTestId('contact-detail');
        const history = detail.getByTestId('change-history');
        await history.waitFor();
        await d.pause(700);
        await look(d, history, 3600);
        await page.keyboard.press('Escape');
        await detail.waitFor({ state: 'hidden' });
        await d.pause(400);
        await look(d, page.getByRole('link', { name: '정기 변경 주간' }), 3000);
        await d.pause(500);
      },
    },
  ],
};
