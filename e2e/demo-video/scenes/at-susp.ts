import type { Page } from '@playwright/test';

import type { Director } from '../helpers.ts';
import type { Chapter } from './types.ts';
import { AT_SLUG, FAST, asLee, asOps, look } from './at-common.ts';

/** 공문의 "자동이체 서비스 중단"이 실제로는 계좌이동 업무라는 이야기(발표자료 7장, SHY 2026-11-22). 관련 업무는 자동이체통합관리다. */
const WORKS = ['계좌이동'];
/** 등록 장면에서 만든 건의 주소(확정 장면이 연다). */
let registeredPath = '';

const REASON = '계정계 시스템 증설 작업으로 자동이체 계좌이동 처리가 중단됩니다.';

/** 서울 기준 오늘에서 days 만큼 뒤의 날짜. */
function seoulDate(days: number) {
  const d = new Date(Date.now() + 9 * 3_600_000 + days * 86_400_000);
  return { year: d.getUTCFullYear(), month: d.getUTCMonth() + 1, day: d.getUTCDate() };
}

/** 일시 칸에서 날짜를 고르고 시·분을 넣는다(04-suspensions.ts 와 같은 방식). */
async function pickDateTime(d: Director, page: Page, label: string, date: ReturnType<typeof seoulDate>, hhmm: string) {
  await d.click(page.getByRole('button', { name: label, exact: true }), { settle: 40, steps: 4 });
  const dropdown = page.getByRole('dialog').filter({ has: page.getByRole('spinbutton') });
  await dropdown.waitFor({ state: 'visible' });
  const dayButton = dropdown.getByRole('button', { name: `${date.day} ${date.month}월 ${date.year}`, exact: true });
  if (!(await dayButton.isVisible())) await d.click(dropdown.getByRole('button').nth(2), { settle: 40, steps: 4 });
  await d.click(dayButton, { settle: 40, steps: 4 });
  await dropdown.getByRole('spinbutton').first().click();
  await page.keyboard.type(hhmm, { delay: 8 });
  await d.pause(60);
  await d.click(dropdown.getByRole('button').last(), { settle: 40, steps: 4 });
  await dropdown.waitFor({ state: 'hidden' });
}

/** 가상의 한 쪽짜리 공문 PDF(녹화 때 즉석에서 만든다. 실제 문서가 아니다). */
function samplePdf(): Buffer {
  const text = '(Sample notice - virtual document)';
  const stream = `BT /F1 18 Tf 72 720 Td ${'(' + text.replace(/[()]/g, '') + ')'} Tj ET`;
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
    `<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
  ];
  let out = '%PDF-1.4\n';
  const offsets: number[] = [];
  objects.forEach((o, i) => {
    offsets.push(out.length);
    out += `${i + 1} 0 obj\n${o}\nendobj\n`;
  });
  const xref = out.length;
  out += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  for (const off of offsets) out += `${String(off).padStart(10, '0')} 00000 n \n`;
  out += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(out, 'latin1');
}

/** susp 일시중단(대본 v1.8, 약 20초): 기관이 계좌이동을 골라 등록 → 결제원 운영이 확인·확정 → 운영·관제 매터모스트 채널. */
export const atSusp: Chapter = {
  id: 'susp',
  title: '일시중단',
  scenes: [
    {
      id: 'at-susp-register',
      kind: 'app',
      summary: '신한은행(이준호) → 일시중단 등록 → 대상 업무 계좌이동(관련 업무 자동이체통합관리) → 일시·이유·연락처·공문 PDF → 저장 → 검토 대기',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/suspensions/new`);
        await page.getByRole('heading', { name: '일시중단 등록' }).waitFor();
        const works = page.getByTestId('suspension-businesses');
        await d.pause(300);
        for (const name of WORKS) await d.click(works.getByRole('checkbox', { name, exact: true }), FAST);
        await page.getByTestId('suspension-businesses-count').getByText(`고른 업무 ${WORKS.length}개`).waitFor();
        await d.pause(500);
        await pickDateTime(d, page, '중단 시작', seoulDate(1), '2300');
        await pickDateTime(d, page, '중단 종료', seoulDate(2), '0200');
        await d.type(page.getByRole('textbox', { name: '중단 이유(작업 내용)' }), REASON, { delay: 2 });
        await page.getByRole('textbox', { name: '연락처' }).first().fill('010-0000-3101');
        await page.locator('input[type="file"]').setInputFiles({ name: '신한은행_일시중단_공문.pdf', mimeType: 'application/pdf', buffer: samplePdf() });
        const files = page.getByRole('list', { name: '올릴 파일' });
        await files.getByText('신한은행_일시중단_공문.pdf').waitFor();
        await look(d, files, 300);
        await d.click(page.getByRole('button', { name: '저장', exact: true }), FAST);
        await page.waitForURL(new RegExp(`/${AT_SLUG}/suspensions/\\d+$`));
        registeredPath = new URL(page.url()).pathname;
        const status = page.getByTestId('suspension-status');
        await status.getByText('검토 대기').waitFor();
        await look(d, status, 1500);
      },
    },
    {
      id: 'at-susp-confirm',
      kind: 'app',
      summary: '결제원 운영(김태호) → 검토 대기 건 → 확인 시작 → 조치 쓰고 확정 → 확정하기 → 주간 취합 보내기(운영·관제 매터모스트 채널, 방금 확정한 신한은행 건)',
      run: async (d, page) => {
        await asOps(d);
        if (!registeredPath) throw new Error('at-susp-register 를 먼저 찍어야 합니다(같은 녹화에서).');
        await page.goto(registeredPath);
        await page.getByRole('heading', { name: '신한은행 일시중단' }).waitFor();
        const status = page.getByTestId('suspension-status');
        const startDialog = page.getByRole('dialog', { name: '확인 시작' });
        await d.clickUntil(page.getByRole('button', { name: '확인 시작' }), startDialog, FAST);
        await d.clickUntil(startDialog.getByRole('button', { name: '확인 시작' }), status.getByText('검토 중'), FAST);
        const handover = page.getByRole('dialog', { name: '조치 쓰고 확정' });
        await d.clickUntil(page.getByRole('button', { name: '조치 쓰고 확정' }), handover, FAST);
        await d.type(handover.getByRole('textbox', { name: '조치 및 인수인계 사항' }), '23:00 신한은행 계좌이동 요청 전송 보류, 재개 연락 오면 재처리.', { delay: 2 });
        await d.clickUntil(handover.getByRole('button', { name: '확정하기' }), status.getByText('확정', { exact: true }), FAST);
        await d.pause(400);
        // 확정 건은 운영·관제 매터모스트 채널로 모아 보낸다(평소에는 정해진 때 자동, 시연은 지금 보낸다).
        await page.goto(`/${AT_SLUG}/suspensions`);
        const confirmSend = page.getByRole('dialog', { name: '주간 취합 보내기' });
        await d.clickUntil(page.getByRole('button', { name: '주간 취합 보내기' }), confirmSend, FAST);
        const digest = page.getByRole('dialog', { name: '주간 취합을 보냈습니다' });
        await d.clickUntil(confirmSend.getByRole('button', { name: '보내기' }), digest.getByTestId('digest-body'), FAST);
        await look(d, digest.getByTestId('digest-body'), 3000);
      },
    },
  ],
};
