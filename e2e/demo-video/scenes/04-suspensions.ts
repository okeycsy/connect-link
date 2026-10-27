import { asKftc, asOrg, type Chapter } from './types.ts';

/** 기관 담당자가 등록할 때 고르는 업무. 정보서비스·지급결제 묶음을 넘나들며 여러 개를 고른다. */
const WORKS_TO_PICK = ['계좌통합관리', '자동이체통합관리', 'CMS', '오픈뱅킹'];

/** 03 일시중단: 기관이 여러 업무를 골라 등록하고, 결제원이 확인·확정하며 인수인계가 남는다. */
export const suspensions: Chapter = {
  id: 'suspensions',
  title: '03 일시중단',
  scenes: [
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
      summary:
        `KB국민은행(김민준)으로 전환 → 일시중단 목록의 "다른 기관 일정" 강조 → "일시중단 등록" → "대상 업무" 칸을 비춘 채 ${WORKS_TO_PICK.join('·')} 네 업무를 묶음을 넘나들며 체크 → "고른 업무 4개" 설명 → 결제원(홍성민)으로 전환 → "예정" 목록 → 신한은행(검토 대기) → "확인 시작" → 검토 중 → "조치 쓰고 확정" 창을 비춘 채 조치·인수인계 사항 입력 → 확정하기 → 확정 상태 강조`,
      narration:
        '기관 담당자는 다른 기관의 확정 일정도 함께 보고, 전문 응답이 없을 때 참고합니다. 등록할 때는 중단되는 업무를 묶음을 넘나들며 하나하나 골라, 누락 없이 정확하게 결제원에 알립니다. 결제원 담당자는 검토 대기 건을 확인하고, 조치와 인수인계 사항을 써서 확정합니다.',
      run: async (d, page) => {
        await asOrg(d);
        await page.goto('/account-info/suspensions');
        const others = page.getByTestId('other-schedules');
        await others.waitFor();
        await d.pause(400);
        await d.focus(others, {
          note: '기관 담당자 화면. <b>다른 기관의 확정 일정</b>도 함께 보여, 전문 응답이 없을 때 참고합니다.',
          scale: 1.25,
          hold: 1500,
        });
        await d.click(page.getByRole('link', { name: '일시중단 등록' }));
        await page.getByRole('heading', { name: '일시중단 등록' }).waitFor();
        const works = page.getByTestId('suspension-businesses');
        await works.waitFor();
        await d.focus(works, {
          note: '중단되는 업무를 <b>하나하나 골라</b> 등록합니다. 묶음을 넘나들며 여러 개.',
          scale: 1.12,
          center: 0.5,
        });
        for (const name of WORKS_TO_PICK) {
          await d.click(works.getByRole('checkbox', { name, exact: true }));
          await d.pause(220);
        }
        await page.getByTestId('suspension-businesses-count').getByText(`고른 업무 ${WORKS_TO_PICK.length}개`).waitFor();
        await d.pause(300);
        await d.note(`<b>고른 업무 ${WORKS_TO_PICK.length}개</b> — 누락 없이, 정확하게. 관련 업무 레일마다 결제원 담당자에게 알립니다.`);
        await d.pause(1600);
        await d.unfocus();

        await asKftc(d);
        await page.goto('/account-info/suspensions?period=UPCOMING');
        const table = page.getByTestId('suspension-table');
        await table.waitFor();
        await d.pause(400);
        await d.caption('결제원 담당자 화면. 기관이 등록하면 <b>검토 대기</b>.');
        await d.pause(500);
        await d.click(table.getByRole('button').filter({ hasText: '신한은행' }).first());
        await page.getByRole('heading', { name: '신한은행 일시중단' }).waitFor();
        await d.pause(300);
        const startDialog = page.getByRole('dialog', { name: '확인 시작' });
        await d.clickUntil(page.getByRole('button', { name: '확인 시작' }), startDialog);
        const status = page.getByTestId('suspension-status');
        await d.clickUntil(startDialog.getByRole('button', { name: '확인 시작' }), status.getByText('검토 중'));
        await d.caption(null);
        const handover = page.getByRole('dialog', { name: '조치 쓰고 확정' });
        await d.clickUntil(page.getByRole('button', { name: '조치 쓰고 확정' }), handover);
        await d.focus(handover, { note: '<b>확인 시작</b>, 조치 쓰고 <b>확정</b>. 인수인계 사항이 담당자마다 남습니다.', scale: 1.1 });
        await d.type(handover.getByRole('textbox', { name: '조치 및 인수인계 사항' }), '23:00 신한은행 연계 차단, 재개 연락 받으면 해제 후 상황실 공유.', {
          delay: 10,
        });
        await d.pause(250);
        await d.clickUntil(handover.getByRole('button', { name: '확정하기' }), status.getByText('확정', { exact: true }));
        await d.unfocus();
        await d.focus(status, { note: '<b>확정</b>. 확인자와 인수인계 사항이 상세에 남습니다.', scale: 1.3, hold: 900 });
        await d.pause(200);
      },
    },
  ],
};
