import type { Page } from '@playwright/test';

import type { Director } from '../helpers.ts';
import { asKftc, asOrg, type Chapter } from './types.ts';

/** 기관 담당자가 등록할 때 고르는 업무. 정보서비스·지급결제 묶음을 넘나들며 여러 개를 고른다. */
const WORKS_TO_PICK = ['계좌통합관리', '자동이체통합관리', 'CMS', '오픈뱅킹'];

/** 장면에서 새로 등록하는 일시중단의 중단 이유. */
const REASON = '계정계 시스템 증설 작업으로 4개 업무 전문 응답이 중단됩니다.';

/** 서울 기준 오늘에서 days 만큼 뒤의 날짜. 서버 시간대(Asia/Seoul)와 같은 날짜를 고르기 위해 쓴다. */
function seoulDate(days: number) {
  const d = new Date(Date.now() + 9 * 3_600_000 + days * 86_400_000);
  return { year: d.getUTCFullYear(), month: d.getUTCMonth() + 1, day: d.getUTCDate() };
}

/**
 * 일시 칸을 눌러 달력에서 날짜를 고르고 시·분을 넣는다(e2e/tests/suspensions.spec.ts 와 같은 방식).
 * 다음 달이면 달을 넘긴다. 커서가 보이게 Director 로 누른다.
 */
async function pickDateTime(
  d: Director,
  page: Page,
  label: string,
  date: { year: number; month: number; day: number },
  hour: string,
  minute: string,
) {
  await d.click(page.getByRole('button', { name: label, exact: true }), { settle: 40, steps: 4 });
  const dropdown = page.getByRole('dialog').filter({ has: page.getByRole('spinbutton') });
  await dropdown.waitFor({ state: 'visible' });
  const dayButton = dropdown.getByRole('button', { name: `${date.day} ${date.month}월 ${date.year}`, exact: true });
  if (!(await dayButton.isVisible())) {
    // 머리의 세 버튼은 이전 달, 달 이름, 다음 달 순서다.
    await d.click(dropdown.getByRole('button').nth(2), { settle: 40, steps: 4 });
  }
  await d.click(dayButton, { settle: 40, steps: 4 });
  await dropdown.getByRole('spinbutton').first().click();
  await page.keyboard.type(hour + minute, { delay: 8 });
  await d.pause(60);
  await d.click(dropdown.getByRole('button').last(), { settle: 40, steps: 4 });
  await dropdown.waitFor({ state: 'hidden' });
}

/**
 * 03 일시중단: 기관이 여러 업무를 골라 직접 등록하면 결제원 담당자에게 매터모스트 알림이 바로 가고,
 * 결제원이 확인·확정하면 그 주 확정 건이 주간 취합으로 운영·관제 매터모스트 채널에 모아 나간다.
 */
export const suspensions: Chapter = {
  id: 'suspensions',
  title: '03 일시중단',
  scenes: [
    {
      id: 'c-suspensions',
      kind: 'card',
      duration: 5200,
      card: {
        tone: 'light',
        chapter: '03 일시중단',
        title: '누락업무 없이 등록,\n업무팀, 운영팀 담당자 매터모스트 알림까지',
        titleSize: 54,
      },
      narration: '셋째, 거의 매주 발생하는 참가기관의 일시중단.',
    },
    {
      id: 'suspensions',
      kind: 'app',
      summary:
        `KB국민은행(김민준)으로 전환 → 일시중단 목록의 "다른 기관 일정" 강조(서비스 취급기관이면 다른 기관 일시중단도 함께) → "일시중단 등록" → 중단 시작(내일 23:00)·종료(다음 날 02:00)를 달력에서 고르고 중단 이유·담당자 연락처 입력 → "대상 업무" 칸을 비춘 채 ${WORKS_TO_PICK.join('·')} 네 업무를 묶음을 넘나들며 체크 → "고른 업무 4개" 설명 → 저장 → 상세의 "검토 대기" 강조(관련 업무 결제원 담당자에게 메일·매터모스트 알림) → 결제원(홍성민)으로 전환 → 발송함에서 방금 기록된 "매터모스트 · … 일시중단이 등록되었습니다" 줄 강조 → 방금 등록된 건의 상세를 열어 "확인 시작" → 검토 중 → "조치 쓰고 확정" 창을 비춘 채 조치·인수인계 사항 입력 → 확정하기 → 확정 상태 강조 → 목록의 "주간 취합 보내기" → 확인 창 강조(평소에는 수·금 15시에 자동) → 보내기 → "주간 취합을 보냈습니다" 창(운영·관제 매터모스트 채널, 그 주 확정 건의 기관·중단 기간·조치) 강조 → 실제 매터모스트 채널 캡처`,
      run: async (d, page) => {
        // ① 기관 담당자: 다른 기관 일정을 보고, 일시중단을 직접 등록한다.
        await asOrg(d);
        d.say('서비스 취급기관은 다른 기관 일정도 함께 볼 수 있습니다.');
        await page.goto('/account-info/suspensions');
        const others = page.getByTestId('other-schedules');
        await others.waitFor();
        await d.pause(250);
        await d.focus(others, {
          note: '서비스 취급기관인 경우, <b>다른 기관의 일시중단</b>도 함께 볼 수 있습니다.',
          scale: 1.25,
          hold: 2500,
        });
        await d.click(page.getByRole('link', { name: '일시중단 등록' }));
        await page.getByRole('heading', { name: '일시중단 등록' }).waitFor();
        await d.pause(300);
        d.say('일시중단 등록은 정해진 양식으로. 일시와 이유를 적고,');
        await pickDateTime(d, page, '중단 시작', seoulDate(1), '23', '00');
        await pickDateTime(d, page, '중단 종료', seoulDate(2), '02', '00');
        await d.type(page.getByRole('textbox', { name: '중단 이유(작업 내용)' }), REASON, { delay: 2 });
        d.say('중단되는 업무를 하나하나 골라 누락 없이 등록합니다.');
        await d.type(page.getByRole('textbox', { name: '연락처' }).first(), '010-0000-2101', { delay: 2 });
        const works = page.getByTestId('suspension-businesses');
        await d.focus(works, {
          note: '중단되는 업무를 <b>하나하나 골라</b> 등록합니다. 묶음을 넘나들며 여러 개.',
          scale: 1.12,
          center: 0.5,
        });
        for (const name of WORKS_TO_PICK) {
          await d.click(works.getByRole('checkbox', { name, exact: true }));
          await d.pause(30);
        }
        await page.getByTestId('suspension-businesses-count').getByText(`고른 업무 ${WORKS_TO_PICK.length}개`).waitFor();
        await d.pause(100);
        await d.note(`<b>고른 업무 ${WORKS_TO_PICK.length}개</b> — 누락 없이, 정확하게.`);
        await d.pause(250);
        await d.unfocus();
        d.say('저장하면 바로 검토 대기가 됩니다.');
        await d.click(page.getByRole('button', { name: '저장', exact: true }));
        await page.waitForURL(/\/account-info\/suspensions\/\d+$/);
        await page.getByRole('heading', { name: 'KB국민은행 일시중단' }).waitFor();
        const status = page.getByTestId('suspension-status');
        await status.getByText('검토 대기').waitFor();
        const detailUrl = page.url();
        await d.pause(250);
        await d.focus(status, {
          note: '저장하면 바로 <b>검토 대기</b>. 관련 업무 결제원 담당자에게 메일과 <b>매터모스트 알림</b>이 갑니다.',
          scale: 1.3,
          hold: 900,
        });

        // ② 결제원 담당자: 등록 알림(매터모스트)을 발송함에서 확인하고, 그 건을 열어 확정한다.
        await asKftc(d);
        d.say('관련 업무 우리 원 담당자에게는 매터모스트 알림이 바로 갑니다.');
        await page.goto('/account-info/demo/outbox');
        const outbox = page.getByTestId('outbox-table').locator('tbody tr');
        const alertRow = outbox.filter({ hasText: '매터모스트' }).filter({ hasText: '일시중단이 등록되었습니다' }).first();
        await alertRow.waitFor();
        await d.pause(250);
        await d.focus(alertRow, {
          note: '기관이 등록하면 결제원 담당자에게 <b>매터모스트 알림</b>. 시연 환경은 발송함에 기록합니다.',
          scale: 1.15,
          hold: 1400,
        });
        // 알림을 받은 결제원 담당자가 그 건을 연다.
        await page.goto(detailUrl);
        await page.getByRole('heading', { name: 'KB국민은행 일시중단' }).waitFor();
        await status.getByText('검토 대기').waitFor();
        await d.pause(150);
        const startDialog = page.getByRole('dialog', { name: '확인 시작' });
        await d.clickUntil(page.getByRole('button', { name: '확인 시작' }), startDialog);
        await d.clickUntil(startDialog.getByRole('button', { name: '확인 시작' }), status.getByText('검토 중'));
        const handover = page.getByRole('dialog', { name: '조치 쓰고 확정' });
        await d.clickUntil(page.getByRole('button', { name: '조치 쓰고 확정' }), handover);
        d.say('우리 원 운영 담당자는 조치와 인수인계 사항을 써서 확정합니다.');
        await d.focus(handover, { note: '<b>확인 시작</b>, 조치 쓰고 <b>확정</b>. 인수인계 사항이 담당자마다 남습니다.', scale: 1.1 });
        await d.type(handover.getByRole('textbox', { name: '조치 및 인수인계 사항' }), '23:00 KB국민은행 연계 차단, 재개 연락 오면 해제 후 상황실 공유.', {
          delay: 5,
        });
        await d.pause(150);
        await d.clickUntil(handover.getByRole('button', { name: '확정하기' }), status.getByText('확정', { exact: true }));
        await d.unfocus();
        await d.focus(status, { note: '<b>확정</b>. 확인자와 인수인계 사항이 상세에 남습니다.', scale: 1.3, hold: 1650 });

        // ③ 주간 취합: 그 주 확정 건을 수·금마다 운영·관제 매터모스트 채널로 모아 보낸다. 시연에서는 "주간 취합 보내기"로 지금 보낸다.
        d.say('확정된 건은 정해진 때마다 모아 운영·관제 채널로 보냅니다.');
        await page.goto('/account-info/suspensions');
        const confirmSend = page.getByRole('dialog', { name: '주간 취합 보내기' });
        await d.clickUntil(page.getByRole('button', { name: '주간 취합 보내기' }), confirmSend);
        await d.focus(confirmSend, {
          note: '그 주에 <b>확정</b>된 일시중단은 <b>수·금</b>마다 모아 운영·관제 <b>매터모스트 채널</b>로 자동 전달. 지금은 바로 보냅니다.',
          scale: 1.1,
          hold: 250,
        });
        const digest = page.getByRole('dialog', { name: '주간 취합을 보냈습니다' });
        await d.clickUntil(confirmSend.getByRole('button', { name: '보내기' }), digest.getByTestId('digest-body'));
        await d.pause(100);
        await d.focus(digest, {
          note: '방금 확정한 KB국민은행 건까지 <b>한 번에</b>. 기관, 중단 기간, 조치만 간추려 보냅니다.',
          scale: 1.05,
          hold: 750,
        });
        await d.pause(150);
      },
    },
    {
      // 실제 운영·관제 매터모스트 채널에 전달된 모습(SHY 캡처, 봇 이름은 가렸다). 취합 창과 같은 내용이 채널에 그대로 간다.
      id: 'suspensions-mattermost',
      kind: 'card',
      duration: 4600,
      card: {
        tone: 'light',
        image: { src: '/assets/mattermost-digest.png', width: 1500, frame: true },
        subtitle: '운영·관제 매터모스트 채널에 실제로 전달된 모습',
      },
      narration: '매터모스트 채널에는 이렇게 갑니다.',
    },
  ],
};
