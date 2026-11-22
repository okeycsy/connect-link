import type { Chapter } from './types.ts';
import { AT_SLUG, FAST, asLim, asMoon, look } from './at-common.ts';

/**
 * prep 준비: 대본 "연락처 메뉴만 열면 → 기관 담당자가 직접 고친 최신 명부, 바뀐 이력 → 6개월이 지나면 기관에 다시 묻고 →
 * 정기 변경도 알아서 요청" 순서. 마지막은 결제원 연락처 화면에서 현행화 진행률이 오른 것을 보고 끝낸다.
 */
export const atPrep: Chapter = {
  id: 'prep',
  title: '준비 — 현행화된 연락처',
  scenes: [
    {
      id: 'at-prep-list',
      kind: 'app',
      summary:
        '결제원(문서준) 홈 → 사이드바 "연락처" → 기관별 담당자 표 → KB국민은행 남도윤 상세: 최근 확인일·기관 담당자 장서윤이 고친 변경 이력(백승호 → 남도윤)',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/home`);
        await d.pause(900);
        await d.click(page.getByRole('navigation').getByRole('link', { name: '연락처' }).first(), FAST);
        const table = page.getByTestId('contact-table');
        await table.getByText('장서윤').waitFor();
        await d.pause(700);
        await look(d, table.getByRole('button', { name: 'KB국민은행 장서윤' }), 500);
        await d.click(table.getByRole('button', { name: 'KB국민은행 남도윤' }), FAST);
        const detail = page.getByTestId('contact-detail');
        const history = detail.getByTestId('change-history');
        await history.waitFor();
        await d.pause(500);
        await look(d, history, 2600);
      },
    },
    {
      id: 'at-prep-reconfirm',
      kind: 'app',
      summary:
        '삼성증권 임채원(최근 확인일 6개월 지남) 로그인 → "담당자 정보를 확인해 주세요" 창 → 변동 없음 → 닫기 → 연락처의 정기 변경 주간 안내 → 현행화 완료',
      run: async (d, page) => {
        await asLim(d);
        await page.goto(`/${AT_SLUG}/home`);
        const modal = page.getByTestId('contact-reconfirm');
        await modal.waitFor();
        await d.pause(1300);
        await d.click(modal.getByRole('button', { name: '변동 없음' }).first(), FAST);
        await modal.getByText('확인 완료').first().waitFor();
        await d.pause(700);
        await d.click(modal.getByRole('button', { name: '닫기' }), FAST);
        await modal.waitFor({ state: 'hidden' });
        await d.click(page.getByRole('navigation').getByRole('link', { name: '연락처' }).first(), FAST);
        const banner = page.getByTestId('campaign-banner');
        await banner.waitFor();
        await d.pause(900);
        await d.click(banner.getByRole('button', { name: '현행화 완료' }), FAST);
        await banner.getByText('현행화 완료 ·').waitFor();
        await d.pause(1100);
      },
    },
    {
      id: 'at-prep-done',
      kind: 'app',
      summary: '결제원(문서준) 연락처 → 정기 변경 주간 진행률(완료 3/6) → 삼성증권 임채원 최근 확인일 오늘',
      run: async (d, page) => {
        await asMoon(d);
        await page.goto(`/${AT_SLUG}/contacts`);
        const table = page.getByTestId('contact-table');
        await table.getByText('임채원').waitFor();
        await d.pause(700);
        await look(d, page.getByTestId('campaign-banner').getByText(/완료 \d+ \/ 대상/), 1300);
        await look(d, table.getByRole('button', { name: '삼성증권 임채원' }), 1500);
      },
    },
  ],
};
