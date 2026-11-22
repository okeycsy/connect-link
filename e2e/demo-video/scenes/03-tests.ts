import { SEEDED_TEST, asKftc, asOrg, type Chapter } from './types.ts';

/** 02 기관 테스트: 기관이 결과를 올리고, 결제원이 의견·미니 수행률을 남기고, 수행률은 저절로. */
export const tests: Chapter = {
  id: 'tests',
  title: '02 기관 테스트',
  scenes: [
    {
      id: 'c-tests',
      kind: 'card',
      duration: 1800,
      card: { tone: 'light', chapter: '02 기관 테스트', title: '결과는 기관이, 의견은 결제원이,\n수행률은 저절로.' },
      narration: '둘째, 기관 테스트.',
    },
    {
      id: 'tests-org',
      kind: 'app',
      summary:
        'KB국민은행(김민준)으로 전환 → 기관 테스트 "본인계좌 일괄지급정지 전문 개선 2차" → 요구사항 005 강조 → "정상" 클릭 → "결과 저장" → 제목과 "우리 기관 수행률 · 완료" 강조',
      run: async (d, page) => {
        await asOrg(d);
        await page.goto('/account-info/tests/1');
        await page.getByRole('heading', { name: SEEDED_TEST }).waitFor();
        await d.pause(300);
        const item = page.getByTestId('item-005');
        d.say('기관 담당자가 요구사항마다 결과를 저장하면,');
        await d.focus(item, { note: '요구사항마다 <b>정상·오류</b>를 고르고', scale: 1.2 });
        await d.click(item.getByText('정상', { exact: true }));
        await d.pause(800);
        await d.unfocus();
        await d.click(page.getByRole('button', { name: /결과 저장/ }));
        await d.pause(300);
        d.say('수행률이 바로 집계됩니다.');
        await d.focus([page.getByRole('heading', { name: SEEDED_TEST }), page.getByTestId('plan-progress')], {
          note: '저장하면 <b>수행률이 바로</b> 집계됩니다.',
          scale: 1.3,
          hold: 400,
        });
        await d.pause(100);
      },
    },
    {
      id: 'tests-kftc',
      kind: 'app',
      summary:
        '결제원(홍성민)으로 전환 → 같은 테스트 "기관별 현황" 표 강조(기관별 수행률·마지막 등록) → 요구사항 004 "의견" → 의견 창을 비춘 채 미니 수행률 80 · 특이사항 입력 → 저장 → "결제원(홍성민 과장) 미니 수행률 80%" 강조',
      run: async (d, page) => {
        await asKftc(d);
        await page.goto('/account-info/tests/1');
        const targets = page.getByTestId('target-table');
        await targets.waitFor();
        await d.pause(100);
        d.say('우리 원 개발 담당자는 기관별 현황을 한눈에 보고,');
        await d.focus(targets, { note: '결제원은 <b>기관별 현황</b>을 한눈에.', scale: 1.2, hold: 1400 });
        const item = page.getByTestId('item-004');
        await item.scrollIntoViewIfNeeded();
        await d.pause(100);
        const dialog = page.getByRole('dialog');
        await d.clickUntil(item.getByRole('button', { name: '004 의견' }), dialog.getByRole('textbox', { name: '미니 수행률' }));
        await d.focus(dialog, { note: '요구사항마다 <b>의견과 미니 수행률</b>을 남깁니다.', scale: 1.1 });
        d.say('요구사항마다 의견과 미니 수행률을 남깁니다.');
        await d.type(dialog.getByRole('textbox', { name: '미니 수행률' }), '80');
        await d.type(dialog.getByRole('textbox', { name: '특이사항' }), '9999 건만 다시 봐 주세요.', {
          delay: 2,
        });
        await d.pause(50);
        await d.clickUntil(dialog.getByRole('button', { name: '저장' }), item.getByTestId('item-feedback'));
        await d.unfocus();
        await d.focus(item.getByTestId('item-feedback'), {
          note: '결제원 의견이 기관 결과 옆에 남습니다. 기관이 고치면 <b>다시 확인</b>.',
          scale: 1.3,
          hold: 50,
        });
      },
    },
  ],
};
