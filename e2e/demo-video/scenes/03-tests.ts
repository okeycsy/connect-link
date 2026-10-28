import { SEEDED_TEST, asKftc, asOrg, type Chapter } from './types.ts';

/** 02 기관 테스트: 기관이 결과를 올리고, 결제원이 의견·미니 수행률을 남기고, 수행률은 저절로. */
export const tests: Chapter = {
  id: 'tests',
  title: '02 기관 테스트',
  scenes: [
    {
      id: 'c-tests',
      kind: 'card',
      duration: 3000,
      card: { tone: 'light', chapter: '02 기관 테스트', title: '결과는 기관이, 의견은 결제원이,\n수행률은 저절로.' },
      narration: '둘째, 기관 테스트.',
    },
    {
      id: 'tests-org',
      kind: 'app',
      summary:
        'KB국민은행(김민준)으로 전환 → 기관 테스트 "본인계좌 일괄지급정지 전문 개선 2차" → 요구사항 005 강조 → "정상" 클릭 → "결과 저장" → 제목과 "우리 기관 수행률 · 완료" 강조',
      narration:
        '개발 건마다 테스트 페이지가 열립니다. 기관 담당자는 요구사항마다 결과를 고르고 저장합니다. 수행률은 바로 집계되고, 결제원 담당자에게 매터모스트로 알립니다.',
      run: async (d, page) => {
        await asOrg(d);
        await page.goto('/account-info/tests/1');
        await page.getByRole('heading', { name: SEEDED_TEST }).waitFor();
        await d.pause(500);
        const item = page.getByTestId('item-005');
        await d.focus(item, { note: '요구사항마다 <b>정상·오류</b>를 고르고', scale: 1.2 });
        await d.click(item.getByText('정상', { exact: true }));
        await d.pause(500);
        await d.unfocus();
        await d.click(page.getByRole('button', { name: /결과 저장/ }));
        await d.pause(500);
        await d.focus([page.getByRole('heading', { name: SEEDED_TEST }), page.getByTestId('plan-progress')], {
          note: '저장하면 <b>수행률이 바로</b> 집계됩니다.',
          scale: 1.3,
          hold: 1300,
        });
        await d.pause(200);
      },
    },
    {
      id: 'tests-kftc',
      kind: 'app',
      summary:
        '결제원(홍성민)으로 전환 → 같은 테스트 "기관별 현황" 표 강조(기관별 수행률·마지막 등록) → 요구사항 004 "의견" → 의견 창을 비춘 채 미니 수행률 80 · 특이사항 입력 → 저장 → "결제원(홍성민 과장) 미니 수행률 80%" 강조 → 003 "답글" 펼치기(기관↔결제원 답글) 강조',
      narration:
        '결제원 담당자는 기관별 현황을 한눈에 보고, 요구사항마다 의견과 미니 수행률을 남깁니다. 기관이 다시 고치면 "다시 확인"이 붙고, 같은 칸에서 답글로 이어집니다.',
      run: async (d, page) => {
        await asKftc(d);
        await page.goto('/account-info/tests/1');
        const targets = page.getByTestId('target-table');
        await targets.waitFor();
        await d.pause(400);
        await d.focus(targets, { note: '결제원은 <b>기관별 현황</b>을 한눈에.', scale: 1.2, hold: 1100 });
        const item = page.getByTestId('item-004');
        await item.scrollIntoViewIfNeeded();
        await d.pause(400);
        const dialog = page.getByRole('dialog');
        await d.clickUntil(item.getByRole('button', { name: '004 의견' }), dialog.getByRole('textbox', { name: '미니 수행률' }));
        await d.focus(dialog, { note: '요구사항마다 <b>의견과 미니 수행률</b>을 남깁니다.', scale: 1.1 });
        await d.type(dialog.getByRole('textbox', { name: '미니 수행률' }), '80');
        await d.type(dialog.getByRole('textbox', { name: '특이사항' }), '응답코드 매핑은 확인했습니다. 9999 건만 다시 봐 주세요.', {
          delay: 12,
        });
        await d.pause(300);
        await d.clickUntil(dialog.getByRole('button', { name: '저장' }), item.getByTestId('item-feedback'));
        await d.unfocus();
        await d.focus(item.getByTestId('item-feedback'), {
          note: '결제원 의견이 기관 결과 옆에 남습니다. 기관이 고치면 <b>다시 확인</b>.',
          scale: 1.3,
          hold: 900,
        });
        const third = page.getByTestId('item-003');
        await third.scrollIntoViewIfNeeded();
        await d.click(third.getByRole('button', { name: '003 답글' }));
        const comments = third.getByTestId('item-comments');
        await comments.waitFor();
        await d.focus(comments, { note: '같은 칸에서 <b>답글</b>로 이어집니다.', scale: 1.3, hold: 900 });
        await d.pause(200);
      },
    },
  ],
};
