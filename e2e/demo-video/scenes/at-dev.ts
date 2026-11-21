import type { Chapter } from './types.ts';
import { AT_SLUG, AT_TEST, asLee, asMoon, atIds, look } from './at-common.ts';

const SHINHAN = '088';

/** dev 개발팀: 테스트데이터 사전 요청 → 기관이 생성 완료·결과 등록 → 결제원이 수행률을 보고 의견 → 기관이 답글. */
export const atDev: Chapter = {
  id: 'dev',
  title: '개발팀 — 기관 테스트',
  scenes: [
    {
      id: 'at-dev-request',
      kind: 'app',
      summary: '결제원(문서준) → 기관 테스트 "자동이체 해지 결과 배치파일 개선" 결과 탭 → 테스트데이터 사전 요청(생성 완료 2/6) → "미생성 기관에 다시 요청" → 보냈다는 안내',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/tests/${atIds().testPlanId}`);
        await page.getByRole('heading', { name: AT_TEST }).waitFor();
        const bar = page.getByTestId('test-data-request-bar');
        await bar.waitFor();
        await d.pause(500);
        await look(d, [bar, page.getByTestId('target-table')]);
        await d.pause(1600);
        await d.click(bar.getByRole('button', { name: /미생성 기관에 다시 요청/ }));
        const sent = page.getByText(/테스트데이터 요청 메일을 보냈습니다/);
        await sent.waitFor();
        await look(d, sent, 1800);
      },
    },
    {
      id: 'at-dev-org',
      kind: 'app',
      summary: '신한은행(이준호) → 같은 테스트 → 테스트데이터 "생성 완료"와 메모 저장 → 002 정상, 003 오류 + 특이사항 "18시 파일 레코드 수 불일치" → 결과 저장',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/tests/${atIds().testPlanId}`);
        const card = page.getByTestId('test-data');
        await card.waitFor();
        await d.pause(400);
        await look(d, card);
        await d.click(card.getByText('생성 완료', { exact: true }));
        await d.type(card.getByRole('textbox', { name: '메모' }), '사유코드별 해지 테스트 계좌 3개 생성', { delay: 15 });
        await d.clickUntil(card.getByRole('button', { name: '테스트데이터 저장' }), card.getByText('신한은행(이준호 과장) ·'));
        await d.pause(500);
        const items = ['002', '003'].map((n) => page.getByTestId(`item-${n}`));
        await look(d, items);
        await d.click(items[0].getByText('정상', { exact: true }));
        await d.click(items[1].getByText('오류', { exact: true }));
        await d.type(items[1].getByRole('textbox', { name: '003 특이사항' }), '18시 파일 레코드 수 불일치', { delay: 25 });
        await d.pause(200);
        await d.clickUntil(page.getByRole('button', { name: /결과 저장/ }), page.getByTestId('target-progress').getByText(/^[1-9]\d*%$/));
        await d.pause(300);
        await look(d, page.getByTestId('plan-progress'), 1600);
      },
    },
    {
      id: 'at-dev-feedback',
      kind: 'app',
      summary: '결제원(문서준) → 같은 테스트 기관별 현황에서 신한은행 수행률 → 신한은행 003 에 결제원 의견(미니 수행률 50, "레이아웃 v1.2 로 다시 확인") 저장',
      run: async (d, page) => {
        await asMoon(d);
        const ids = atIds();
        await page.goto(`/${AT_SLUG}/tests/${ids.testPlanId}`);
        const targets = page.getByTestId('target-table');
        await targets.waitFor();
        const row = targets.locator('tr').filter({ hasText: '신한은행' });
        await d.pause(400);
        await look(d, row, 1800);
        await d.click(row.getByText('신한은행').first());
        await page.waitForURL(new RegExp(`org=${ids.orgIds[SHINHAN]}`));
        const item = page.getByTestId('item-003');
        await item.getByText('18시 파일 레코드 수 불일치').waitFor();
        await d.centerOn(item, 0.45);
        const dialog = page.getByRole('dialog');
        await d.clickUntil(item.getByRole('button', { name: '003 의견' }), dialog.getByRole('textbox', { name: '미니 수행률' }));
        await look(d, dialog);
        await d.type(dialog.getByRole('textbox', { name: '미니 수행률' }), '50');
        await d.type(dialog.getByRole('textbox', { name: '특이사항' }), '레이아웃 v1.2 로 다시 확인해 주세요.', { delay: 15 });
        await d.clickUntil(dialog.getByRole('button', { name: '저장' }), item.getByTestId('item-feedback'));
        await look(d, item.getByTestId('item-feedback'), 1600);
      },
    },
    {
      id: 'at-dev-comment',
      kind: 'app',
      summary: '신한은행(이준호) → 같은 화면에서 우리 기관 수행률과 003 의 결제원 의견 확인 → 답글 "수정본으로 재테스트하겠습니다" 달기',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/tests/${atIds().testPlanId}`);
        const item = page.getByTestId('item-003');
        await item.getByTestId('item-feedback').waitFor();
        // 우리 기관 수행률과 결제원 의견을 같은 화면에서 본다.
        await look(d, page.getByTestId('plan-progress'), 1200);
        await d.centerOn(item, 0.45);
        await look(d, item.getByTestId('item-feedback'), 1200);
        await look(d, item);
        await d.click(item.getByRole('button', { name: '003 답글' }));
        await d.type(item.getByRole('textbox', { name: '003 답글 입력' }), '수정본으로 재테스트하겠습니다.', { delay: 30 });
        await d.clickUntil(item.getByRole('button', { name: '답글 달기' }), item.getByTestId('item-comment'));
        await d.pause(1800);
      },
    },
  ],
};
