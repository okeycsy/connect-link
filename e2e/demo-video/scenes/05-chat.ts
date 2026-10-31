import { URGENT_MESSAGE, asKftc, asOrg, type Chapter } from './types.ts';

/** 04 소통 채널: 장애가 나면 @기관을 불러 긴급 확인을 요청하고, 기관이 누른 확인이 기관 이름으로 남는다. */
export const chat: Chapter = {
  id: 'chat',
  title: '04 소통 채널',
  scenes: [
    {
      id: 'c-chat',
      kind: 'card',
      duration: 1400,
      card: { tone: 'light', chapter: '04 소통 채널', title: '장애가 나면 @기관을 부르고,\n기관이 확인합니다.' },
      narration: '넷째, 소통 채널.',
    },
    {
      id: 'chat',
      kind: 'app',
      summary:
        '결제원(홍성민) 소통 채널 → "계좌통합관리 단체방" → 입력창을 비춘 채 "@KB" → "@KB국민은행" 선택 → 긴급 확인 요청 입력 → 보내기 → 보낸 메시지 강조 → KB국민은행(김민준)으로 전환 → 같은 방의 그 메시지를 비춘 채 "확인" 클릭 → 메시지 아래 "KB국민은행 1" 설명',
      run: async (d, page) => {
        await asKftc(d);
        await page.goto('/account-info/chat');
        const rooms = page.getByRole('complementary', { name: '채팅방 목록' });
        const groupRoom = rooms.getByRole('link', { name: /계좌통합관리 단체방/ });
        await groupRoom.waitFor();
        await d.click(groupRoom);
        const messages = page.getByTestId('chat-messages');
        await messages.getByText('@결제원', { exact: true }).waitFor();
        const input = page.getByRole('textbox', { name: '메시지 입력' });
        d.say('장애가 나면 단체방에서 기관을 불러 긴급 확인을 요청합니다.');
        await d.focus(input, { note: '장애가 나면 단체방에서 <b>@기관</b>을 불러 긴급 확인을 요청합니다.', scale: 1.15, pad: 16 });
        await d.click(input);
        await page.keyboard.type('@KB', { delay: 25 });
        await d.click(page.getByRole('option', { name: '@KB국민은행' }));
        await page.keyboard.type(' ' + URGENT_MESSAGE, { delay: 4 });
        await d.pause(150);
        await d.click(page.getByRole('button', { name: '보내기' }));
        const sent = messages.getByTestId('chat-message').last();
        await sent.getByText('@KB국민은행', { exact: true }).waitFor();
        await d.pause(100);
        await d.focus(sent, { note: '<b>@KB국민은행</b>을 불러 긴급 확인 요청.', scale: 1.3, hold: 250 });

        await asOrg(d);
        d.say('기관 담당자가 확인을 누르면 기관 이름으로 남습니다.');
        await page.goto('/account-info/chat');
        await d.click(rooms.getByRole('link', { name: /계좌통합관리 단체방/ }));
        // 샘플 메시지에도 "내 계좌 한눈에 조회" 가 있어, 방금 보낸 긴급 메시지에만 있는 문구로 찾는다.
        const urgent = messages.getByTestId('chat-message').filter({ hasText: '응답 지연 장애' }).last();
        await urgent.waitFor();
        await d.pause(100);
        await d.focus(urgent, { note: 'KB국민은행 담당자가 <b>확인</b>을 누르면,', scale: 1.35 });
        await d.clickUntil(urgent.getByRole('button', { name: '확인', exact: true }), urgent.getByTestId('chat-ack'));
        await d.pause(250);
        await d.note('메시지 아래 <b>기관 이름</b>으로 남습니다. 전화로 하나씩 확인하던 일.');
        await d.pause(750);
        await d.unfocus();
        await d.pause(100);
      },
    },
  ],
};
