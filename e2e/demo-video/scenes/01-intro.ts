import type { Chapter } from './types.ts';

/** 문제 제기 → 소개 → 로그인·홈. 심사 기준의 "업무 관련성"과 "확산 가능성"에 답한다. */
export const intro: Chapter = {
  id: 'intro',
  title: '문제와 소개',
  scenes: [
    {
      id: 'p3',
      kind: 'card',
      // 첫 장면. 발표자가 직접 내레이션을 얹을 수 있게 약 10초 머문다(첫 녹화에 붙는 1~2초 지연을 뺀 값).
      duration: 8800,
      card: {
        tone: 'light',
        kicker: '업무 하나에서만, 해마다',
        kickerSize: 44,
        stat: {
          value: 198,
          prefix: '약',
          unit: '시간',
          label: '메일을 하나하나 열고 이를 내부망에서 취합하는 데 걸리는 시간',
        },
      },
      narration: '(발표자 내레이션) 업무 하나에서만, 해마다 약 198시간이 메일을 하나하나 열고 내부망에서 취합하는 데 들어갑니다.',
    },
    {
      id: 'intro',
      kind: 'card',
      duration: 3300,
      card: {
        tone: 'light',
        image: { src: '/brand/logo-mascot.png', width: 430 },
        title: '우리 원과 참가기관을 잇는\n하나의 포털',
      },
      narration: '그래서 하나의 포털을 만들었습니다. 연결이입니다.',
    },
    {
      id: 'login-home',
      kind: 'app',
      summary:
        '로그인 화면 → 데모 계정 "홍성민(결제원·현업)" 강조·클릭 → 홈에 "결제원 · 홍성민 과장 · 결제원 담당자" 알약(이후 결제원 시점은 갈색, 기관 시점은 파랑) → 홈의 할 일 카드 4개를 모두 강조 → 왼쪽 메뉴(연락처·의견 요청·기관 테스트·일시중단·소통 채널) 강조',
      narration:
        '결제원 담당자 홍성민이 회사 이메일로 로그인합니다. 로그인하면 지금 할 일만 보입니다. 어느 업무든 같은 메뉴, 같은 구조라 94개 업무로 그대로 넓힐 수 있습니다.',
      run: async (d, page) => {
        await page.goto('/login');
        const demo = page.getByTestId('demo-accounts');
        await demo.waitFor();
        await d.pause(400);
        const hong = page.getByRole('button', { name: /^홍성민 .*로 로그인/ });
        await d.focus(hong, { note: '결제원도, 참가기관도 같은 입구. <b>결제원 담당자 홍성민</b>으로 로그인합니다.', scale: 1.3, hold: 700 });
        await d.click(hong);
        await page.waitForURL(/\/home$/);
        await page.getByRole('heading', { name: /안녕하세요/ }).waitFor();
        await d.who('kftc', '결제원', '홍성민 과장 · 결제원 담당자');
        await d.pause(400);
        const cards = page.locator('[data-testid^="home-card-"]');
        const cardCount = await cards.count();
        await d.focus(Array.from({ length: cardCount }, (_, i) => cards.nth(i)), {
          note: '로그인하면 <b>지금 할 일</b>만 보입니다. 네 칸이 모두 할 일.',
          scale: 1.08,
          hold: 850,
        });
        const nav = page.getByRole('navigation');
        await d.moveTo(nav.getByRole('link', { name: '일시중단', exact: true }), { steps: 10 });
        await d.focus(nav, { note: '어느 업무든 <b>같은 구조</b>. 94개 업무까지 그대로.', scale: 1, hold: 700 });
        await d.pause(150);
      },
    },
  ],
};
