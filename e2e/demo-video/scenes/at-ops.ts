import { request } from '@playwright/test';

import type { Chapter } from './types.ts';
import { AT_OPS_MESSAGE, AT_SLUG, FAST, asLee, asOps, look } from './at-common.ts';

const ROOM = /자동이체통합관리 단체방/;

/** 신한은행보다 먼저 확인을 누른 기관 담당자(화면 밖에서 API 로 누른다). */
const EARLY_ACKS = ['jang.seoyun@kb.example.com', 'ha.yunseul@epost.example.com'];

/** 지연 안내 메시지에 다른 기관 담당자가 [확인]을 누른다. 녹화 화면과 상관없는 별도 세션이다. */
async function ackAs(email: string) {
  const base = process.env.DEMO_BASE_URL ?? 'http://localhost:8080';
  const ctx = await request.newContext({ baseURL: base });
  try {
    await ctx.get('/api/v1/health');
    const token = async () => ({ 'X-XSRF-TOKEN': (await ctx.storageState()).cookies.find((c) => c.name === 'XSRF-TOKEN')?.value ?? '' });
    const login = await ctx.post('/api/v1/auth/login', { data: { email, password: 'demo1234!' }, headers: await token() });
    if (!login.ok()) throw new Error(`로그인 실패 ${email}: ${await login.text()}`);
    const rooms = await (await ctx.get(`/api/v1/chat-rooms?business=${AT_SLUG}`)).json();
    const room = (rooms.content ?? rooms).find((r: { name: string }) => ROOM.test(r.name));
    const messages = await (await ctx.get(`/api/v1/chat-rooms/${room.id}/messages?size=50`)).json();
    const target = (messages.content ?? messages).find((m: { content: string }) => m.content === AT_OPS_MESSAGE);
    if (!target) throw new Error('지연 안내 메시지를 못 찾았습니다');
    const res = await ctx.put(`/api/v1/chat-messages/${target.id}/ack`, { headers: await token() });
    if (!res.ok()) throw new Error(`확인 실패 ${email}: ${await res.text()}`);
  } finally {
    await ctx.dispose();
  }
}

/**
 * ops 운영팀(발표자료 6장): 타 기관 수신 지연으로 해지 요청 파일이 늦게 나가면, 기관마다 전화가 오기 전에
 * 단체방 한 번으로 참여 기관 모두에 알리고, 기관이 누른 [확인]이 기관 이름으로 쌓인다.
 */
export const atOps: Chapter = {
  id: 'ops',
  title: '운영팀 — 소통 채널 지연 안내',
  scenes: [
    {
      id: 'at-ops-notice',
      kind: 'app',
      summary: '결제원 운영(김태호) → 소통 채널 → 자동이체통합관리 단체방 → "[지연 안내] 타 기관 수신 지연으로 …" 입력 → 보내기',
      run: async (d, page) => {
        await asOps(d);
        await page.goto(`/${AT_SLUG}/chat`);
        await d.pause(600);
        const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
        await d.click(rooms.getByRole('link', { name: ROOM }), FAST);
        const messages = page.getByTestId('chat-messages');
        await messages.waitFor();
        const input = page.getByRole('textbox', { name: '메시지 입력' });
        await d.click(input, FAST);
        await page.keyboard.type(AT_OPS_MESSAGE, { delay: 4 });
        await d.click(page.getByRole('button', { name: '보내기' }), FAST);
        const sent = messages.getByTestId('chat-message').filter({ hasText: '[지연 안내]' }).last();
        await sent.waitFor();
        await look(d, sent, 1500);
      },
    },
    {
      id: 'at-ops-ack',
      kind: 'app',
      summary: '신한은행(이준호) → 소통 채널 → 단체방의 지연 안내 → [확인] (KB국민은행·우체국은 먼저 확인)',
      run: async (d, page) => {
        for (const email of EARLY_ACKS) await ackAs(email);
        await asLee(d);
        await page.goto(`/${AT_SLUG}/chat`);
        await d.pause(600);
        const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
        const room = rooms.getByRole('link', { name: ROOM });
        await d.click(room, FAST);
        const notice = page.getByTestId('chat-messages').getByTestId('chat-message').filter({ hasText: '[지연 안내]' }).last();
        await notice.waitFor();
        await d.clickUntil(notice.getByRole('button', { name: '확인', exact: true }), notice.getByTestId('chat-ack').filter({ hasText: '신한은행' }), FAST);
        await d.pause(1200);
      },
    },
    {
      id: 'at-ops-acks',
      kind: 'app',
      summary: '결제원 운영(김태호) → 같은 방 → 지연 안내 아래 KB국민은행·우체국·신한은행 확인이 쌓인 것',
      run: async (d, page) => {
        await asOps(d);
        await page.goto(`/${AT_SLUG}/chat`);
        await d.pause(600);
        const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
        await d.click(rooms.getByRole('link', { name: ROOM }), FAST);
        const notice = page.getByTestId('chat-messages').getByTestId('chat-message').filter({ hasText: '[지연 안내]' }).last();
        const acks = notice.getByTestId('chat-ack');
        await acks.filter({ hasText: '신한은행' }).waitFor();
        await look(d, acks.last(), 2400);
      },
    },
  ],
};
