import type { Chapter } from './types.ts';
import { AT_OPS_MESSAGE, AT_OPS_REPLY, AT_SLUG, asLee, asOps, look } from './at-common.ts';

const ROOM = /자동이체통합관리 단체방/;

/** ops 운영팀: 시행 뒤 모니터링 중 파일이 늦으면 단체방에서 @신한은행을 부르고, 기관이 확인·답한다. */
export const atOps: Chapter = {
  id: 'ops',
  title: '운영팀 — 소통 채널',
  scenes: [
    {
      id: 'at-ops-ask',
      kind: 'app',
      summary: '결제원 운영(김태호) → 소통 채널 → 자동이체통합관리 단체방 → "@신한" → "@신한은행" 선택 → 확인 요청 입력 → 보내기',
      run: async (d, page) => {
        await asOps(d);
        await page.goto(`/${AT_SLUG}/chat`);
        const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
        await d.click(rooms.getByRole('link', { name: ROOM }));
        const messages = page.getByTestId('chat-messages');
        await messages.waitFor();
        const input = page.getByRole('textbox', { name: '메시지 입력' });
        await d.pause(300);
        await look(d, input);
        await d.click(input);
        await page.keyboard.type('@신한', { delay: 60 });
        await d.click(page.getByRole('option', { name: '@신한은행' }));
        await page.keyboard.type(' ' + AT_OPS_MESSAGE, { delay: 12 });
        await d.pause(200);
        await d.click(page.getByRole('button', { name: '보내기' }));
        const sent = messages.getByTestId('chat-message').filter({ hasText: AT_OPS_MESSAGE }).last();
        await sent.waitFor();
        await look(d, sent, 1400);
      },
    },
    {
      id: 'at-ops-answer',
      kind: 'app',
      summary: '신한은행(이준호) → 소통 채널 방 목록의 @ 표시 → 방에 들어가 그 메시지에 "확인" → "13시 20분에 재송신했습니다." 보내기',
      run: async (d, page) => {
        await asLee(d);
        await page.goto(`/${AT_SLUG}/chat`);
        const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
        const room = rooms.getByRole('link', { name: ROOM });
        await room.waitFor();
        await d.pause(400);
        await look(d, room, 1300);
        await d.click(room);
        const messages = page.getByTestId('chat-messages');
        const asked = messages.getByTestId('chat-message').filter({ hasText: AT_OPS_MESSAGE }).last();
        await asked.waitFor();
        await look(d, asked);
        await d.clickUntil(asked.getByRole('button', { name: '확인', exact: true }), asked.getByTestId('chat-ack'));
        await d.pause(300);
        const input = page.getByRole('textbox', { name: '메시지 입력' });
        await d.click(input);
        await page.keyboard.type(AT_OPS_REPLY, { delay: 30 });
        await d.click(page.getByRole('button', { name: '보내기' }));
        await messages.getByTestId('chat-message').filter({ hasText: AT_OPS_REPLY }).waitFor();
        await d.pause(900);
      },
    },
    {
      id: 'at-ops-ack',
      kind: 'app',
      summary: '결제원 운영(김태호) → 같은 방 → 메시지 아래 "신한은행 확인"과 답',
      run: async (d, page) => {
        await asOps(d);
        await page.goto(`/${AT_SLUG}/chat`);
        const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
        await d.click(rooms.getByRole('link', { name: ROOM }));
        const messages = page.getByTestId('chat-messages');
        const asked = messages.getByTestId('chat-message').filter({ hasText: AT_OPS_MESSAGE }).last();
        const answer = messages.getByTestId('chat-message').filter({ hasText: AT_OPS_REPLY }).last();
        await answer.waitFor();
        await d.pause(400);
        await look(d, [asked.getByTestId('chat-ack'), answer], 2600);
      },
    },
  ],
};
