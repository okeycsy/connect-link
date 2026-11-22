import type { Chapter } from './types.ts';
import { AT_SLUG, AT_TEST, FAST, asLee, asMoon, atIds, look } from './at-common.ts';

const SHINHAN = '088';

/** dev 개발팀(대본 v1.8, 약 21초): 테스트데이터 사전 요청 → 기관 결과 등록 → 결제원 미니 수행률·의견 → 기관 답글. */
export const atDev: Chapter = {
  id: 'dev',
  title: '개발팀 — 기관 테스트',
  scenes: [
    {
      id: 'at-dev-request',
      kind: 'app',
      summary: '결제원(문서준) → 기관 테스트 "자동이체 해지 결과 배치파일 개선" → 테스트데이터 사전 요청(생성 완료 2/6) → "미생성 기관에 다시 요청"',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/tests/${atIds().testPlanId}`);
        await d.pause(1100);
        await page.getByRole('heading', { name: AT_TEST }).waitFor();
        const bar = page.getByTestId('test-data-request-bar');
        await bar.waitFor();
        await d.pause(400);
        await d.click(bar.getByRole('button', { name: /미생성 기관에 다시 요청/ }), FAST);
        await page.getByText(/테스트데이터 요청 메일을 보냈습니다/).waitFor();
        await d.pause(1600);
      },
    },
    {
      id: 'at-dev-org',
      kind: 'app',
      summary: '신한은행(이준호) → 테스트데이터 "생성 완료" 저장 → 002 정상, 003 오류 + "18시 파일 레코드 수 불일치" → 결과 저장',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/tests/${atIds().testPlanId}`);
        await d.pause(1100);
        const card = page.getByTestId('test-data');
        await card.waitFor();
        await d.pause(300);
        await d.click(card.getByText('생성 완료', { exact: true }), FAST);
        await d.clickUntil(card.getByRole('button', { name: '테스트데이터 저장' }), card.getByText('신한은행(이준호 과장) ·'), FAST);
        const items = ['002', '003'].map((n) => page.getByTestId(`item-${n}`));
        await d.centerOn(items[0], 0.35);
        await d.click(items[0].getByText('정상', { exact: true }), FAST);
        await d.click(items[1].getByText('오류', { exact: true }), FAST);
        await d.type(items[1].getByRole('textbox', { name: '003 특이사항' }), '18시 파일 레코드 수 불일치', { delay: 12 });
        await d.clickUntil(page.getByRole('button', { name: /결과 저장/ }), page.getByTestId('target-progress').getByText(/^[1-9]\d*%$/), FAST);
        await d.pause(1200);
      },
    },
    {
      id: 'at-dev-feedback',
      kind: 'app',
      summary: '결제원(문서준) → 신한은행 결과 → 003 에 결제원 의견(미니 수행률 50, "레이아웃 v1.2 로 다시 확인해 주세요.")',
      run: async (d, page) => {
        await asMoon(d);
        const ids = atIds();
        await page.goto(`/${AT_SLUG}/tests/${ids.testPlanId}?org=${ids.orgIds[SHINHAN]}`);
        await d.pause(1100);
        const item = page.getByTestId('item-003');
        await item.getByText('18시 파일 레코드 수 불일치').waitFor();
        await d.centerOn(item, 0.45);
        const dialog = page.getByRole('dialog');
        await d.clickUntil(item.getByRole('button', { name: '003 의견' }), dialog.getByRole('textbox', { name: '미니 수행률' }), FAST);
        await d.type(dialog.getByRole('textbox', { name: '미니 수행률' }), '50', { delay: 40 });
        await d.type(dialog.getByRole('textbox', { name: '특이사항' }), '레이아웃 v1.2 로 다시 확인해 주세요.', { delay: 8 });
        await d.clickUntil(dialog.getByRole('button', { name: '저장' }), item.getByTestId('item-feedback'), FAST);
        await d.pause(1300);
      },
    },
    {
      id: 'at-dev-comment',
      kind: 'app',
      summary: '신한은행(이준호) → 같은 화면 003 의 결제원 의견 → 답글 "수정본으로 재테스트하겠습니다."',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/tests/${atIds().testPlanId}`);
        await d.pause(1100);
        const item = page.getByTestId('item-003');
        await item.getByTestId('item-feedback').waitFor();
        await d.centerOn(item, 0.45);
        await d.click(item.getByRole('button', { name: '003 답글' }), FAST);
        await d.type(item.getByRole('textbox', { name: '003 답글 입력' }), '수정본으로 재테스트하겠습니다.', { delay: 14 });
        await d.clickUntil(item.getByRole('button', { name: '답글 달기' }), item.getByTestId('item-comment'), FAST);
        await d.pause(1500);
      },
    },
  ],
};
